# Mora — Backend

Backend do sistema de gestão condominial **Mora**: sete microsserviços em Node e Java,
orquestrados por Docker Compose, com Consul para descoberta de serviço e Traefik como gateway.

Projeto acadêmico de Bacharelado em Sistemas de Informação — PUCPR.

---

## O produto

O Mora atende três públicos com necessidades diferentes, e a divisão dos serviços segue essa
separação mais do que qualquer critério técnico:

- **A plataforma** cadastra condomínios clientes e vende planos.
- **A administração do condomínio** configura estrutura física, taxas, comunicados e usuários.
- **O morador** paga faturas, reserva espaços, autoriza visitantes e fala com a administração.

---

## Arquitetura

As sete APIs são acessadas pelo gateway em `http://localhost:8087`; as portas dos microserviços não são publicadas no host. O frontend e as chamadas entre aplicações usam as rotas descobertas pelo Consul. Consulte a [validação do gateway](docs/VALIDACAO-GATEWAY.md) para rotas, testes e callbacks OAuth.

```
                        Frontend (React + Vite — 5173)
                                     │
                          Traefik (gateway — 8087)
                                     │
        ┌──────────┬──────────┬──────┴─────┬───────────┬──────────┬──────────┐
        │          │          │            │           │          │          │
     auth-api  portaria  comunicacao  ocorrencias  financeiro   plan    meeting
      :3001     :8090      :8094        :8095       :3004     :8093    :8091
        │          │          │            │           │          │          │
     auth_db     mora       mora   mora_ocorrencias mora_financeiro mora_plan mora_meeting
                              ▲            │
                              └─ RabbitMQ ─┘  notificações (fila mora.notificacoes)
                                     │
                              gestao-geral :3002
                     (agrega; mora_gestao: preferências)

        Consul :8500  —  descoberta de serviço e roteamento do Traefik
        RabbitMQ :5672 (painel :15672)  —  fila de notificações entre serviços
        PostgreSQL 16 :5433  —  um container, vários bancos
        pgAdmin :5050
```

### Serviços

| Serviço | Responsabilidade | Stack | Porta | Banco |
|---|---|---|---|---|
| `auth-api` | Identidade, perfis, condomínios clientes, convites, portaria de acesso | Node 20 · Express | 3001 | `auth_db` |
| `portaria-service` | Estrutura física, acessos, entregas, chaves, vagas, veículos, reservas | Java 21 · Spring Boot | 8090 | `mora` |
| `comunicacao-service` | Avisos, base de conhecimento, chat e notificações | Java 21 · Spring Boot | 8094 | `mora` |
| `ocorrencias-service` | Reclamações, ordens de serviço e fornecedores | Java 21 · Spring Boot | 8095 | `mora_ocorrencias` |
| `financeiro` | Taxas, rateio, faturas, multas, cobrança via Asaas | Node 20 · Express | 3004 | `mora_financeiro` |
| `plan-service` | Planos comerciais e assinaturas | Java 21 · Spring Boot | 8093 | `mora_plan` |
| `meeting-service` | Assembleias, atas e votações | Java 21 · Spring Boot | 8091 | `mora_meeting` |
| `gestao-geral` | Agrega indicadores dos demais para os painéis; guarda as preferências dos painéis | Node 20 · Express | 3002 | `mora_gestao` |

> `vagas-service` foi **incorporado ao `portaria-service`** (decisão D11) e o diretório foi
> removido. O código continua no histórico do Git: `git show e6d607a:services/vagas-service`.

**`portaria-service` e `comunicacao-service` compartilham o banco `mora`.** Não é acidente: os
avisos e artigos nasceram no portaria e foram migrados para o comunicacao sem mover o esquema,
porque mover não entregaria nada que o usuário não tenha hoje.

---

## Perfis de acesso

São **sete**, em três camadas. A camada define o alcance, não o poder.

| Camada | Perfis | Alcance |
|---|---|---|
| Plataforma | `ADMIN_GERAL` | Todos os condomínios |
| Condomínio | `ADMIN_SINDICO`, `PORTEIRO`, `TERCEIRO` | Um condomínio |
| Unidade | `MORADOR`, `DONO_ALUGUEL`, `CONVIDADO` | Uma unidade — exigem `unidadeId` |

`CONVIDADO` e `TERCEIRO` **não acessam o sistema**: existem para serem cadastrados e registrados
na portaria.

A distinção entre proprietário e inquilino não vive no perfil, e sim na flag
`responsavelFinanceiro` — que é o que a cobrança precisa saber.

### Autenticação

JWT HS256 emitido pelo `auth-api` e validado por todos os demais com o mesmo segredo. As claims
são `{ id, perfil, tokenVersion, email, condominioId, unidadeId, nome }`.

**`tokenVersion` é o que faz o logout valer.** `POST /api/auth/logout` incrementa o contador no
usuário, e todo token emitido antes passa a ser recusado — em qualquer navegador, em qualquer
serviço. Sem isso, "sair" só apagaria o token da aba atual.

**O escopo vem sempre da claim, nunca do corpo ou da query.** Um síndico que passe o
`condominioId` de outro condomínio na URL recebe os dados do próprio.

---

## Como subir

Requisitos: Docker Desktop e Node 20. Java e Maven **não** são necessários — os serviços Java
compilam dentro do container.

```bash
cp docker/.env.example docker/.env
docker compose -f docker/docker-compose.yml up -d --build
```

**Clonar não basta.** Alguns arquivos ficam fora do Git por conterem segredo, e o
`meeting-service` nem sobe sem o dele. A lista completa, com o que cada ausência quebra, está em
[`docs/ARQUIVOS-NECESSARIOS.md`](docs/ARQUIVOS-NECESSARIOS.md).

### Banco de dados

As tabelas são criadas e evoluídas pelas **migrações de cada serviço**, que rodam quando ele
sobe: Flyway nos serviços Java, Umzug no auth-api e node-pg-migrate no financeiro. Não há SQL
para aplicar à mão. Um volume vazio vira um banco completo com o `up`, e um banco antigo recebe
só o que lhe falta. Como funciona e como criar uma migração nova:
[`docs/MIGRACOES.md`](docs/MIGRACOES.md).

O `init-databases.sql` só cria os bancos vazios, e **só roda em volume novo**. Em máquina que já
subiu a stack antes, o Postgres ignora o `docker-entrypoint-initdb.d`, e um banco acrescentado
depois não aparece. Para esses casos há os scripts avulsos em `docker/criar-banco-*.sql`.

### Conferir que subiu

```bash
docker compose -f docker/docker-compose.yml ps
curl http://localhost:8087/api/health
```

Consul em `http://localhost:8500` mostra quem se registrou; o painel do Traefik, em
`http://localhost:8080`, mostra quais rotas ele está publicando.

### Dados de exemplo

```bash
npm run seed           # limpa e recria 3 condomínios completos
npm run seed:manter    # só acrescenta, sem limpar
```

Precisa da stack no ar. O script instala as próprias dependências (`pg`, `bcryptjs`) em
`scripts/`, que tem `package.json` próprio — a raiz não tem dependência nenhuma. As contas
criadas, uma de cada perfil, estão no topo de `scripts/seed-dev.mjs`.

### Documentação da API

**`http://localhost:8087/docs`** reúne a documentação de todos os serviços numa página só, pelo gateway:

- no topo, um sumário com o que cada serviço faz, quem responde por ele, o banco e quantas rotas tem
  (lido ao vivo da especificação de cada um, então serviço fora do ar aparece como fora do ar);
- abaixo, o Swagger UI com um seletor dos serviços. Toda rota tem sumário, descrição e exemplo do
  corpo e da resposta.

Os exemplos usam os dados do seed (condomínio Residencial Parque Verde, Bloco A, apartamento 101):
com o seed aplicado, o **Try it out** funciona com os próprios exemplos. Para as rotas protegidas,
faça `POST /api/auth/login` no auth-api com uma conta do seed e cole o `token` em **Authorize**.

| Serviço | Especificação |
|---|---|
| auth-api | `/api/auth/docs/openapi.json` |
| portaria-service | `/api/portaria/v3/api-docs` |
| comunicacao-service | `/api/comunicacao/v3/api-docs` |
| ocorrencias-service | `/api/ocorrencias/v3/api-docs` |
| financeiro | `/api/financeiro/docs/openapi.json` |
| gestao-geral | `/api/gestao/docs/openapi.json` |
| plan-service | `/api/plans/v3/api-docs` |
| meeting-service | `/api/meetings/v3/api-docs` |

Nos serviços Node a especificação é escrita à mão (`docs/openapi.js`), e um teste compara com as rotas
do Express: rota nova sem documentação quebra a suíte. Nos Java ela é gerada pelo springdoc a partir
das anotações. O teste de API (`tests-api/`) confere as especificações servidas pelo gateway.

---

## Testes

```bash
cd services/auth-api && npm test              # 441 testes
cd services/auth-api && npm run test:cobertura # com relatório de cobertura
```

Os testes do `auth-api` usam **Vitest + Supertest** e exercitam **as 62 rotas**, uma a uma:

| O que afirmam | |
|---|---|
| Autenticação | 401 sem token, com token malformado, **forjado com outro segredo** e expirado |
| Autorização | cada perfil negado recebe 403; cada perfil permitido não recebe |
| Conta desativada | token válido + conta inativa → 401 |
| `tokenVersion` | token anterior ao logout → 401 |

**A lista de rotas é lida do próprio Express**, não escrita à mão. Uma lista manual envelhece em
silêncio: alguém acrescenta um endpoint, ninguém lembra do teste, e a suíte segue verde afirmando
uma cobertura que já não existe. Perguntando ao router, **endpoint novo sem contrato declarado
quebra os testes** — e quem o escreveu precisa dizer, por escrito, se é público e quem ele recusa.

A suíte roda **com o Postgres desligado**: a decisão de acesso acontece antes de qualquer
consulta. O limite disso é conhecido — as rotas que recortam o resultado pelo condomínio ou pela
unidade de quem pediu dependem do que está gravado, e só são verificáveis com banco de pé.

---

## Documentação

| | |
|---|---|
| [`docs/ESPECIFICACAO-PROJETO.md`](docs/ESPECIFICACAO-PROJETO.md) | Requisitos e escopo |
| [`docs/ARQUITETURA-E-FLUXOS.md`](docs/ARQUITETURA-E-FLUXOS.md) | Decisões de arquitetura |
| [`docs/servicos/`](docs/servicos/) | Um documento por serviço, com o porquê de cada decisão |
| [`docs/MIGRACOES.md`](docs/MIGRACOES.md) | Como cada banco é criado e evoluído, e como criar uma migração |
| [`docs/PENDENCIAS.md`](docs/PENDENCIAS.md) | **O que não está pronto, e o que está errado** |
| [`docs/ARQUIVOS-NECESSARIOS.md`](docs/ARQUIVOS-NECESSARIOS.md) | **Os arquivos que não estão no Git** e sem os quais a stack não sobe |

`PENDENCIAS.md` é o documento a ler antes de mexer em qualquer coisa: ele lista as lacunas
conhecidas, inclusive as de segurança, em vez de deixá-las serem descobertas em produção.

---

## Limitações conhecidas

Estão detalhadas em `docs/PENDENCIAS.md`. As que mudam o que dá para fazer com o sistema hoje:

- **`plan-service` não autentica.** Qualquer requisição alcança qualquer rota dele.
- **`comunicacao-service` aceita a entidade crua no corpo** (`@RequestBody Aviso`) ao criar e
  editar aviso. A checagem de perfil e o recorte por condomínio já existem.
- **A senha do Postgres saiu dos arquivos, mas segue no histórico do Git.** O compose agora exige
  `POSTGRES_PASSWORD` e `JWT_SECRET` no `docker/.env`; quem for publicar o sistema precisa usar
  uma senha diferente da que esteve versionada.

---

## Licença

Projeto acadêmico, sem licença de distribuição.
