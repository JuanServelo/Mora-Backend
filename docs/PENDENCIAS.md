# Pendências do projeto

Levantamento do que falta, medido contra a
[especificação](ESPECIFICACAO-PROJETO.md) e os critérios de avaliação, com o
sistema rodando em 10/09/2026.

Cada afirmação aqui foi verificada em código, em banco ou por requisição — não é
estimativa. Onde há número, ele veio de uma medição.

---

## 1. Critérios de avaliação

| # | Critério | Situação |
|---|---|---|
| 1 | Contém pelo menos 80% do escopo acordado | ⚠️ **Abaixo** |
| 2 | Processo ágil e sprints documentadas | ⚠️ **Existe, fora do repositório** |
| 3 | Frontend bem acabado | ✅ |
| 4 | Backend RESTful com JSON | ✅ |
| 5 | Dados persistidos em banco | ✅ |
| 6 | 2 perfis validados no front **e** no back | ⚠️ **Falha nos serviços Java** |
| 7 | Dashboard com informações, filtros e gráficos | ✅ |
| 8 | Git organizado com participação de todos | ✅ |

Cinco atendidos, três parciais.

---

## 2. O achado mais sério: os serviços Java não autenticam

**Prioridade máxima.** Não é só um critério de nota — são dados de um cliente
acessíveis a outro, e a qualquer pessoa sem conta.

### O que foi demonstrado

Sem nenhum token:

```
GET /apartamentos   → 200
GET /blocos         → 200
GET /areas-comuns   → 200
GET /api/plans      → 200
```

`GET /apartamentos` devolveu **12 apartamentos dos três condomínios**, com bloco
e número. E um morador do Parque Verde pedindo
`?condominioId=cond-vista-mar` recebeu **as 4 unidades do outro cliente**.

Os serviços Node recusam a mesma requisição: 401 sem token, e o morador que pede
outro condomínio recebe o dele.

### Por que acontece

`portaria-service/security/AuthFilter.java` lê o token e, quando ele falha,
deixa passar:

```java
try {
    AuthContext.set(jwtUtil.parse(token));
} catch (Exception ignored) {
    // Token inválido — contexto permanece vazio; service lança 403 se a rota exigir auth
}
chain.doFilter(request, response);
```

O comentário promete um 403 que não existe: em todo o `portaria-service`, **só o
`VeiculoService` consulta o `AuthContext`**. O `plan-service` não tem sequer um
filtro.

### Por que o frontend não cobre

As 62 verificações de perfil do frontend decidem o que a **tela** mostra. Somem
no instante em que alguém usa `curl`, Postman ou a aba de rede do navegador.

O sistema é multi-inquilino: nos serviços Java, o `condominioId` é **um
parâmetro que o cliente escolhe**, não uma regra que o servidor aplica.

### O que fazer

Na ordem, do mais barato ao mais caro:

1. **Rejeitar token ausente ou inválido com 401** — trocar o
   `catch (Exception ignored)` por uma resposta. Sozinho, fecha o buraco maior
2. **Derivar o `condominioId` do token**, ignorando o da query para quem não é
   Admin Geral
3. **Exigir perfil nas rotas de escrita** — hoje qualquer um cadastra bloco e
   apartamento

O `middleware/escopo.js` do `financeiro-service` já faz exatamente isso e serve
de modelo.

---

## 3. Serviços que não existem

| Serviço | Spec | Realidade |
|---|---|---|
| `comunicacao-service` | Node · 3003 · `mora_comunicacao` | ✅ **existe** — ver abaixo |
| `ocorrencias-service` | Java · 8095 · `mora_ocorrencias` | ❌ Nem código nem banco |

### `comunicacao-service` — construído com o que era novo

O caminho escolhido foi criar o serviço **só com o que não existia**, deixando
avisos e base de conhecimento onde já funcionam:

| Recurso | Onde está | Estado |
|---|---|---|
| Avisos | `mora.avisos` + `AvisoController` (7 endpoints, no portaria) | Publica — **segue lá** |
| Base de conhecimento | `mora.artigos_conhecimento` + `ArtigoConhecimentoController` (9 endpoints, no portaria) | Funciona — **segue lá** |
| Confirmação de leitura | `mora_comunicacao.aviso_leituras` | ✅ pronta, sem FK para outro banco |
| Chat | `conversas`, `conversa_participantes`, `mensagens` | ✅ pronto |
| Notificações | `notificacoes` + rota interna por `X-Servico-Token` | ✅ pronto |

45 checagens de ponta a ponta passam contra a stack real. Detalhes em
[docs/servicos/comunicacao-service.md](servicos/comunicacao-service.md).

**O que ainda falta para a centralização ser real:** o `financeiro` continua
gravando notificação na tabela dele. A rota interna está pronta e verificada, mas
o corte exige mudar também a leitura no frontend — senão a notificação passa a
viver em dois lugares.

### `ocorrencias-service` — RF-14

Nada existe. Há `reclamacoes` no `auth-api`, que cobre o registro da ocorrência,
mas não ordem de serviço, responsável nem prazo.

### Serviço fora da especificação

O **`vagas-service`** roda na 8092 e **não aparece na especificação** — as vagas
estão atribuídas ao portaria no documento (RF-5). Ou a spec incorpora o serviço,
ou o serviço se funde ao portaria.

---

## 4. Requisitos funcionais

### Completos (8)

RF-1 autenticação · RF-2 usuários e vínculos · RF-3 clientes · RF-4 planos e
assinaturas · RF-8 chaves · RF-12 comunicados · RF-13 mensagens e notificações ·
RF-18 dashboards

> **RF-12 conta como completo, com uma ressalva:** os três pedaços que a spec
> pede existem — avisos, artigos e confirmação de leitura — mas repartidos entre
> o `portaria-service` e o `comunicacao-service`. Funciona; a spec descreve tudo
> num serviço só.

### Parciais — a lacuna exata

| RF | O que a spec pede e **não existe** |
|---|---|
| **5** Estrutura | Vagas em serviço separado, não no portaria como documentado |
| **6** Acessos | **Pré-autorização pelo morador** — nenhum código |
| **7** Entregas | **Notificação ao destinatário** — o `EntregaService` não notifica |
| **9** Funcionários | **Controle de jornada** — há tabelas de turno, falta o fluxo |
| **10** Reservas | **Não há tabela de reservas em banco nenhum.** Só o cadastro de área comum. Faltam solicitação, aprovação, conflito, antecedência e taxa |
| **11** Assembleias | Videoconferência existe (`meet_link`, `google_event_id`), mas `tb_poll_vote` guarda **`usuario_id`, não `unidade_id`** — a apuração por unidade que a spec pede é impossível |
| **14** Ocorrências | **Ordem de serviço, responsável e prazo** |
| **16** Cobranças | **Multas** — tabela existe, sem model, service ou endpoint |

### Planejados que seguem planejados

| RF | Estado real |
|---|---|
| **15** Contratos de locação | **Só a tabela.** Sem model, service, endpoint ou tela |
| **17** Prestação de contas | **Só as tabelas** `lancamentos` e `prestacao_contas` |

### A especificação está desatualizada

O documento marca **RF-16 como planejado**, mas ele tem regras de taxa, rateio,
fração ideal, contas de consumo, fechamento de competência, faturas, baixa
manual, KPIs e gateway de pagamento. Falta só a multa.

E lista o `financeiro-service` fora dos serviços em operação, quando ele roda na
3004 com 14 tabelas e migrações versionadas.

| | Spec diz | Real |
|---|---|---|
| Implementados | 3 | **8** |
| Parciais | 10 | 8 |
| Planejados | 5 | 2 |

**44% completos**, ou ~67% contando parcial como meio ponto. Ainda abaixo dos 80%
do critério 1, mas o `comunicacao-service` moveu dois RFs de uma vez.

---

## 5. Processo ágil — a correção mais barata da lista

**Sprints 0 a 5 estão documentadas**, com cronograma, PERT-CPM, atas de reunião,
matriz de comunicação e registro de mudanças.

O problema: está tudo em `atividades/`, que **nunca foi commitado** — aparece
como `??` no git. O avaliador não vai ver.

**Um commit resolve.** Converte um critério parcial em atendido.

---

## 6. Dívidas menores

| Item | Onde | Impacto |
|---|---|---|
| Senha real do Postgres como valor padrão | `docker/docker-compose.yml`, 6 ocorrências, já no histórico | Segredo em repositório compartilhado. Rotacionar, depois trocar por placeholder |
| Identidades git duplicadas | Ambos os repos | 5 pessoas aparecem como 9 autores. Um `.mailmap` consolida |
| `logs/plan-service.log` versionado | Backend | Gera conflito toda vez que alguém roda o serviço |
| Rótulos de perfil antigos | `Perfil.jsx` diz *"Lessee, Occupant e Guest"* | Modelo de 11 perfis virou 6 há tempo |
| `HISTORIAS-DE-USUARIO.md` | `docs/` | Ainda descreve 11 perfis e 28 RFs |
| Numeração de RF divergente | `HISTORIAS-DE-USUARIO.md` × `docs/servicos/` | Deslocada em um. Alguém vai implementar o escopo errado achando que acertou |
| Sem `trust proxy` no `auth-api` | Limitador de login | Atrás do Traefik, **todos os usuários dividem um balde de 10 tentativas** |
| Chave PIX não cadastrada | Conta Asaas | Boleto funciona; PIX é recusado até cadastrar |

---

## 7. Ordem sugerida

### Agora

1. **Fechar a autenticação nos serviços Java** — segurança real e critério 6
2. **Commitar `atividades/`** — critério 2, custo de um commit

### Depois, por custo-benefício

Os três primeiros ficam no `financeiro-service`, que já tem migrações,
autorização e padrão estabelecido — saem muito mais rápido que abrir serviço
novo:

3. **Multas (RF-16)** — fecha o RF por inteiro; tabela e padrão prontos
4. **Contratos de locação (RF-15)** — tabela pronta, e "responsável financeiro"
   já existe no `auth-api`
5. **Prestação de contas (RF-17)** — duas tabelas prontas; é CRUD mais publicação
6. **Voto por unidade (RF-11)** — trocar `usuario_id` por `unidade_id`
7. **Cortar as notificações do `financeiro` para o `comunicacao-service`** — a
   rota interna já existe; falta trocar a leitura no frontend

### Decisões de escopo pendentes

- **`comunicacao-service` e `ocorrencias-service` vão existir?** Se não, corrigir
  a especificação em vez de deixá-los como dívida documentada
- **`vagas-service` entra na spec ou funde no portaria?**
- **Avisos e base de conhecimento migram** do portaria para o
  `comunicacao-service`, ou ficam onde estão? Hoje funcionam lá, e só a
  confirmação de leitura mora no serviço novo
- **Reservas (RF-10)** é um dos três pilares declarados do produto e não tem uma
  linha de código. Entra no escopo ou sai da visão?
