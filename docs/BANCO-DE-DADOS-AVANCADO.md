# Uso avançado do banco de dados

Como o Mora atende ao critério:

> O sistema faz uso avançado de tecnologias de banco de dados, SQL ou ORM:
> a) uso de *grouped queries* em dashboards;
> b) uso correto de índices para reforçar regras de negócio;
> c) *stored procedures*, geonavegação ou recursos específicos do banco de dados.

O foco são os dois painéis de gestão. O do **Admin Geral** (gestão geral) mostra a plataforma
inteira e a receita das assinaturas. Os do **síndico** (gestão) mostram o condomínio dele. O
`gestao-geral` monta os dois. Ele não tem banco e não conta listas: cada número chega pronto,
agregado em SQL pelo serviço que guarda o dado.

O banco é PostgreSQL 16, um banco lógico por serviço. Todo objeto citado aqui nasce por
migração (ver [MIGRACOES.md](MIGRACOES.md)).

---

## a) Consultas agrupadas nos painéis

| Painel | Número | Consulta | Onde |
|---|---|---|---|
| Admin Geral · plataforma | Condomínios por status; criados por mês | `GROUP BY status`; `GROUP BY date_trunc('month', "createdAt")` | `auth-api/services/estatisticasService.js` |
| Admin Geral · plataforma | Usuários por perfil, por status e por condomínio; ocorrências por status | `GROUP BY perfil`, `status`, `"condominioId"` (Sequelize `fn('COUNT')` + `group`) | idem |
| Admin Geral · receita | MRR, receita por plano, evolução mensal, resumo por cliente | Função `receita_plataforma`: `GROUP BY` por plano e por mês, `generate_series`, `DISTINCT ON` | `plan-service`, `V3__receita_plataforma.sql` |
| Síndico · estratégico | Inadimplência mês a mês | Função `kpis_faturas_por_mes`: `GROUP BY` competência, `COUNT ... FILTER`, `SUM ... FILTER` | `financeiro`, `20261028120000_recursos-do-banco.sql` |
| Síndico · estratégico | Multas aplicadas, canceladas e valor por mês | `GROUP BY` mês no fuso de São Paulo, `FILTER` | `financeiro/models/multasModel.js` (`contarPorMes`) |
| Síndico · estratégico | Ocorrências abertas e resolvidas por mês, por categoria, tempo médio de resolução | `GROUP BY` mês e categoria, `AT TIME ZONE`, `AVG(EXTRACT(EPOCH ...))` | `auth-api/services/estatisticasService.js` (`estatisticasReclamacoes`) |
| Síndico · operacional | Situação atual das faturas da competência | `COUNT(*) FILTER (WHERE status = ...)` | `financeiro/models/faturasModel.js` (`kpisPorCompetencia`) |
| Síndico · operacional | Confirmações de leitura de cada aviso | JPQL `GROUP BY al.avisoId`, uma consulta para todos os avisos | `comunicacao-service`, `AvisoLeituraRepository.contarPorAviso` |

### O que mudou para fechar o item

**Receita da plataforma.** Antes, o `gestao-geral` baixava todas as assinaturas e somava em
JavaScript. Com 36 meses de janela, era a base inteira trafegando para virar meia dúzia de números.
Agora `GET /api/assinaturas/receita` devolve o painel pronto, calculado pela função
`receita_plataforma`, e o `gestao-geral` só acrescenta o nome de cada condomínio e quem não
assina nada. Esses dois dados vêm do `auth-api`, outro banco, e esse cruzamento não pode ser
um JOIN.

**Inadimplência do painel estratégico.** Antes, havia uma requisição e uma consulta por mês: 12
meses eram 12 idas ao financeiro. Agora `GET /admin/faturas/kpis/por-mes` faz uma consulta
agrupada, e `generate_series` garante uma linha por mês, inclusive os sem fatura.

Os dois foram conferidos contra o cálculo antigo com os dados do banco local. A evolução de 12
meses, o MRR e os clientes ativos deram os mesmos valores.

---

## b) Índices que reforçam regra de negócio

| Regra | Índice ou restrição | Banco |
|---|---|---|
| O mês não fecha duas vezes para a mesma unidade, mas a fatura cancelada libera a reemissão | `uq_fatura_competencia`: único **parcial**, `WHERE status <> 'CANCELADA'` | `mora_financeiro` |
| Uma unidade tem no máximo um contrato de locação vigente | `uq_contrato_ativo`: único **parcial**, `WHERE ativo` | `mora_financeiro` |
| **Contratos da mesma unidade não se sobrepõem no tempo**, ativos ou encerrados | `ex_contrato_periodo_unico`: **restrição de exclusão** GiST, `daterange(inicio, fim) WITH &&` | `mora_financeiro` |
| Um condomínio tem no máximo uma assinatura ativa, com histórico livre | `uq_assinatura_ativa`: único **parcial**, `WHERE status = 'ATIVA'` | `mora_plan` |
| O mesmo evento não gera duas notificações | `uq_notificacoes_chave_unica`: único **parcial**, `WHERE chave_unica IS NOT NULL` | `mora` (comunicacao) |
| Cada pessoa confirma a leitura de um aviso uma vez | `UNIQUE (aviso_id, usuario_id)` | `mora` (comunicacao) |
| O webhook do gateway reenviado não dá baixa duas vezes | `UNIQUE (evento_id)` em `webhook_eventos` | `mora_financeiro` |
| Dois condomínios podem ter um "Bloco A" ou um "Salão de festas"; o mesmo condomínio, não | `UNIQUE (nome, "condominioId")` em `blocos` e `areas_comuns` | `mora` (portaria) |

Os índices parciais e a restrição de exclusão não existem no Hibernate nem no Sequelize. Por isso
estão em migração SQL.

A aplicação traduz a recusa do banco em resposta clara: `23505` (único) e `23P01` (exclusão)
viram **409** com a mensagem certa para cada caso.

### A restrição de exclusão, em detalhe

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE contratos_locacao
  ADD CONSTRAINT ex_contrato_periodo_unico
  EXCLUDE USING gist (
    condominio_id WITH =,
    unidade_id    WITH =,
    daterange(inicio, fim, '[)') WITH &&
  );
```

`uq_contrato_ativo` só olha o flag `ativo`. Nada impedia cadastrar um contrato com início dentro
do período de um já encerrado, e a unidade ficaria com dois inquilinos responsáveis pelo mesmo
mês. A restrição de exclusão compara os períodos de todos os contratos da unidade.

`btree_gist` permite combinar a igualdade dos textos com a sobreposição de intervalos no mesmo
índice. O intervalo é `[)`: o dia do fim é o dia da troca, e contrato sem fim vai até o infinito.

---

## c) Recursos específicos do PostgreSQL

| Recurso | Para quê | Onde |
|---|---|---|
| **Função SQL** `receita_plataforma(meses, plano)` devolvendo `jsonb` | Painel de receita do Admin Geral pronto no banco | `plan-service`, `V3__receita_plataforma.sql` |
| **Função SQL** `kpis_faturas_por_mes(condominio, meses)` devolvendo tabela | Inadimplência mês a mês do síndico | `financeiro`, `20261028120000_recursos-do-banco.sql` |
| **Gatilho PL/pgSQL** `trg_fatura_encerrada` | Fatura paga não muda nem é apagada; cancelada não volta | idem |
| **Gatilho PL/pgSQL** `trg_itens_fatura_paga` | Itens de fatura paga não mudam | idem |
| **Restrição de exclusão** com GiST e `btree_gist` | Contratos sem períodos sobrepostos | idem |
| **Tipos de intervalo** (`daterange`) e operadores `&&`, `@>` | Vigência de assinatura e de contrato | as duas migrações |
| `generate_series`, `DISTINCT ON`, `FILTER`, `jsonb_build_object`, `jsonb_agg` | Séries sem buracos, uma linha por condomínio, contagens condicionais, JSON montado no banco | as duas funções |
| Índices parciais (`CREATE UNIQUE INDEX ... WHERE`) | Item b | vários |
| `AT TIME ZONE 'America/Sao_Paulo'` | Uma ocorrência às 22h do dia 31 é daquele mês | `auth-api`, `financeiro` |
| `gen_random_uuid()`, `TIMESTAMPTZ`, `JSONB` | Chaves, datas com fuso, dados flexíveis | vários |

### Gatilho: fatura paga é imutável

A fatura paga é o recibo do morador e entra na prestação de contas. Todo `UPDATE` do código já
filtra `status IN ('ABERTA', 'EM_ATRASO')`, mas isso é disciplina de quem escreve a consulta. O
gatilho transforma essa disciplina em regra. Nenhum caminho altera valor, competência ou baixa de
uma fatura paga, nem a apaga: rota, job, webhook ou alguém com `psql` na mão.

```sql
IF OLD.status = 'PAGA' AND
   (NEW.status, NEW.valor_centavos, NEW.valor_pago_centavos, NEW.pago_em, ...)
   IS DISTINCT FROM
   (OLD.status, OLD.valor_centavos, OLD.valor_pago_centavos, OLD.pago_em, ...)
THEN
  RAISE EXCEPTION 'A fatura % já foi paga e não pode ser alterada.', OLD.id
    USING ERRCODE = 'restrict_violation';
END IF;
```

A recusa sai com SQLSTATE `23001`, e o tratador de erro do financeiro a devolve como **409** com a
mensagem do próprio gatilho. As outras falhas de banco continuam como 500 genérico. O `TRUNCATE`
do seed de desenvolvimento não dispara gatilho de linha.

### Função: receita da plataforma

```sql
-- uma linha por mês da janela, inclusive os sem cliente
janela AS (
  SELECT generate_series(date_trunc('month', current_date) - make_interval(months => n - 1),
                         date_trunc('month', current_date), interval '1 month')::date AS mes
),
-- quem esteve vigente em algum dia do mês conta naquele mês
evolucao AS (
  SELECT j.mes, COUNT(b.id), SUM(b.mensalidade_centavos)
    FROM janela j
    LEFT JOIN base b ON b.vigencia && daterange(j.mes, (j.mes + interval '1 month')::date, '[)')
   GROUP BY j.mes
),
-- uma assinatura por condomínio: a ativa, ou a mais recente
clientes AS (
  SELECT DISTINCT ON (condominio_id) ...
    FROM base
   ORDER BY condominio_id, (status = 'ATIVA') DESC, created_at DESC
)
SELECT jsonb_build_object('indicadores', ..., 'porPlano', ..., 'evolucao', ..., 'clientes', ...)
```

O Java só chama a função (`@Query(nativeQuery = true)` em `AssinaturaRepository`) e devolve o
JSON.

---

## Como verificar

Com a stack no ar:

```bash
# objetos no banco
docker exec postgres psql -U admin -d mora_financeiro -c "\df kpis_faturas_por_mes" -c "select tgname, tgrelid::regclass from pg_trigger where not tgisinternal" -c "select conname, pg_get_constraintdef(oid) from pg_constraint where contype = 'x'"
docker exec postgres psql -U admin -d mora_plan -c "\df receita_plataforma"

# as funções em ação
docker exec postgres psql -U admin -d mora_financeiro -c "select * from kpis_faturas_por_mes('cond-parque-verde', 6)"
docker exec postgres psql -U admin -d mora_plan -c "select jsonb_pretty(receita_plataforma(6, null))"

# o gatilho recusando (dentro de uma transação desfeita, não altera nada)
docker exec postgres psql -U admin -d mora_financeiro -c "begin; update faturas set valor_centavos = 1 where status = 'PAGA'; rollback;"
```

Pela API, através do gateway:

| Rota | Perfil |
|---|---|
| `GET /api/gestao/receita?meses=12` | Admin Geral |
| `GET /api/gestao/painel/estrategico?meses=6` | Síndico |
| `GET /api/assinaturas/receita` | Admin Geral (síndico recebe 403) |
| `GET /api/financeiro/admin/faturas/kpis/por-mes?meses=6` | Síndico |

## Testes

- **SQL, num Postgres descartável:** a função devolve uma linha por mês, inclusive o vazio. Os
  gatilhos recusam alterar, reabrir e apagar fatura paga, reabrir cancelada e incluir item em
  fatura paga. Continuam permitidos: tocar `atualizado_em`, pagar uma aberta e apagar uma aberta
  com itens. A exclusão recusa período sobreposto e aceita o contrato que começa no dia do fim
  do anterior e outra unidade no mesmo período.
- **Receita:** conferida contra o algoritmo antigo, em JavaScript, com os dados do banco local.
  Também testada com troca de plano no meio do mês, assinatura suspensa com fim anterior ao
  início e filtro por plano.
- **Unitários:** `gestao-geral` (31, cinco novos: receita e inadimplência numa chamada) e
  `financeiro` (65).
- **Ponta a ponta:** `tests-api`, 57 requisições e 130 asserções, 0 falhas. Inclui a conferência
  de que as rotas novas estão documentadas.
