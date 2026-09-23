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

```
                        Frontend (React + Vite — 5173)
                                     │
                          Traefik (gateway — 8087)
                                     │
        ┌──────────┬──────────┬──────┴─────┬──────────┬──────────┐
        │          │          │            │          │          │
     auth-api  portaria  comunicacao  financeiro   plan    meeting
      :3001     :8090      :8094        :3004     :8093    :8091
        │          │          │            │          │          │
     auth_db     mora       mora     mora_financeiro mora_plan mora_meeting
                                     │
                              gestao-geral :3002
                          (agrega, não tem banco)

        Consul :8500  —  descoberta de serviço e roteamento do Traefik
        PostgreSQL 16 :5433  —  um container, vários bancos
        pgAdmin :5050
```

### Serviços

| Serviço | Responsabilidade | Stack | Porta | Banco |
|---|---|---|---|---|
| `auth-api` | Identidade, perfis, condomínios clientes, convites, portaria de acesso | Node 20 · Express | 3001 | `auth_db` |
| `portaria-service` | Estrutura física, acessos, entregas, chaves, vagas, veículos, reservas | Java 21 · Spring Boot | 8090 | `mora` |
| `comunicacao-service` | Avisos, base de conhecimento, chat e notificações | Java 21 · Spring Boot | 8094 | `mora` |
| `financeiro` | Taxas, rateio, faturas, multas, cobrança via Asaas | Node 20 · Express | 3004 | `mora_financeiro` |
| `plan-service` | Planos comerciais e assinaturas | Java 21 · Spring Boot | 8093 | `mora_plan` |
| `meeting-service` | Assembleias, atas e votações | Java 21 · Spring Boot | 8091 | `mora_meeting` |
| `gestao-geral` | Agrega indicadores dos demais para o painel do Admin Geral | Node 20 · Express | 3002 | — |

> `vagas-service` foi **incorporado ao `portaria-service`** e está desativado no compose. O
> diretório continua no repositório enquanto a migração dos dados não é confirmada.

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

O `init-databases.sql` cria os bancos, mas **só roda em volume novo**. Em máquina que já subiu a
stack antes, o Postgres ignora o `docker-entrypoint-initdb.d`, e um banco acrescentado depois não
aparece — para esses há os scripts avulsos em `docker/criar-banco-*.sql`.

### Conferir que subiu

```bash
docker compose -f docker/docker-compose.yml ps
curl http://localhost:3001/api/health
```

Consul em `http://localhost:8500` mostra quem se registrou; o painel do Traefik, em
`http://localhost:8080`, mostra quais rotas ele está publicando.

---

## Testes

```bash
cd services/auth-api && npm test              # 422 testes de contrato
cd services/auth-api && npm run test:cobertura # com relatório de cobertura
```

Os testes do `auth-api` usam **Vitest + Supertest** e exercitam **as 61 rotas**, uma a uma:

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
| [`docs/PENDENCIAS.md`](docs/PENDENCIAS.md) | **O que não está pronto, e o que está errado** |
| [`docs/ARQUIVOS-NECESSARIOS.md`](docs/ARQUIVOS-NECESSARIOS.md) | **Os arquivos que não estão no Git** e sem os quais a stack não sobe |

`PENDENCIAS.md` é o documento a ler antes de mexer em qualquer coisa: ele lista as lacunas
conhecidas, inclusive as de segurança, em vez de deixá-las serem descobertas em produção.

---

## Limitações conhecidas

Estão detalhadas em `docs/PENDENCIAS.md`. As que mudam o que dá para fazer com o sistema hoje:

- **`plan-service` não autentica.** Qualquer requisição alcança qualquer rota dele.
- **`comunicacao-service` aceita a entidade crua no corpo** (`@RequestBody Aviso`), e o
  `jwt.secret` tem default inseguro no `application.yml`. A checagem de perfil e o recorte por
  condomínio já existem.
- **A senha do Postgres está versionada** como default no compose e em outros nove arquivos.
  Precisa ser rotacionada junto com a remoção.
- **`node_modules/` está no índice do Git** — 5.881 arquivos. O `.gitignore` já o lista, mas
  arquivo já rastreado não é ignorado.

---

## Licença

Projeto acadêmico, sem licença de distribuição.
