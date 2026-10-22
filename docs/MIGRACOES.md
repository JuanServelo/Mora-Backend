# Migrações do banco de dados

Todo banco do Mora é criado e evoluído por uma ferramenta de migração, que roda quando o
serviço sobe. Ninguém aplica SQL à mão, e um banco vazio vira um banco completo só subindo a
stack.

| Serviço | Banco | Ferramenta | Onde ficam as migrações | Histórico no banco |
|---|---|---|---|---|
| comunicacao-service | `mora` | Flyway | `src/main/resources/db/migration` | `flyway_historico_comunicacao` |
| portaria-service | `mora` | Flyway | `src/main/resources/db/migration` | `flyway_historico_portaria` |
| plan-service | `mora_plan` | Flyway | `src/main/resources/db/migration` | `flyway_historico_plan` |
| meeting-service | `mora_meeting` | Flyway | `src/main/resources/db/migration` | `flyway_historico_meeting` |
| auth-api | `auth_db` | Umzug (o executor do Sequelize) | `migrations/index.js` | `SequelizeMeta` |
| financeiro | `mora_financeiro` | node-pg-migrate | `migrations/*.sql` | `pgmigrations` |

O gestao-geral não tem banco: ele agrega dados dos outros serviços.

O `docker/init-databases.sql` só cria os bancos vazios. Até a adoção das migrações, ele também
criava tabelas do `auth_db` e do `mora`.

---

## Como funciona

**Serviços Java.** O Spring Boot roda o Flyway antes de montar o Hibernate. O Flyway compara
os arquivos `V<n>__descricao.sql` com a tabela de histórico e aplica os que faltam, em ordem.
Depois o Hibernate sobe com `ddl-auto: validate`: ele não cria nada, só confere. Se uma
entidade pedir uma coluna que não existe, o serviço não sobe, e o erro diz qual coluna é. Antes,
com `ddl-auto: update`, o Hibernate criava a coluna por conta própria, e o que ele não sabia
fazer (índice parcial, CHECK de enum com valor novo) ficava para um SQL avulso que cada um
lembrava, ou não, de rodar.

**auth-api.** Na subida, o `server.js` chama o Umzug, que aplica as migrações pendentes da lista
em `migrations/index.js` e grava o nome de cada uma em `SequelizeMeta`. São as mesmas funções de
antes, mas agora cada uma roda uma vez por banco, e não mais a cada boot. A primeira,
`001-estrutura-inicial`, cria as tabelas que vinham do `init-databases.sql`.

**financeiro.** O `server.js` roda o node-pg-migrate antes de escutar a porta. As migrações já
eram versionadas, mas só rodavam com `npm run migrate:up`, à mão; no Docker ninguém rodava. Se
uma migração falhar, o serviço não sobe.

### Banco novo e banco que já existe

As duas situações passam pelo mesmo caminho, e foi isso que guiou o desenho.

- **Banco novo:** tudo é criado pelas migrações, a partir do V1.
- **Banco existente** (a máquina de quem já rodava o projeto): o V1 de cada serviço Java é
  escrito com `IF NOT EXISTS`, e as chaves estrangeiras passam por uma função que pula a chave
  que já existe. As migrações do auth-api já eram idempotentes.
- **Banco atrasado:** depois de cada `CREATE TABLE`, o V1 traz um `ADD COLUMN IF NOT EXISTS`
  para cada coluna. Se o banco não tem uma coluna que a entidade ganhou, ela é criada aqui, como
  o `ddl-auto: update` fazia. Sem isso, o `validate` recusaria a subida. O caso é real: no banco
  usado nos testes, `poll.data_hora_fim` não existia porque o meeting-service não tinha subido
  desde que a coluna entrou na entidade.

Os serviços Java usam `baseline-on-migrate: true` com `baseline-version: 0`. O motivo é o banco
`mora`, dividido entre o comunicacao e o portaria. Com o baseline padrão (versão 1), num banco
novo o segundo serviço a subir encontraria as tabelas do primeiro, acharia o esquema "não
vazio" e pularia o próprio V1. Com a versão 0, o V1 roda sempre, e por isso precisa ser
idempotente. Cada serviço tem a própria tabela de histórico pelo mesmo motivo.

---

## Criar uma migração nova

A regra vale para as três ferramentas: **uma migração que já rodou em algum banco não se edita**.
A mudança vai numa migração nova.

**Serviços Java.** Criar `src/main/resources/db/migration/V<próximo número>__o_que_muda.sql`.
O Flyway guarda o checksum de cada arquivo, e um V1 editado faz o serviço recusar a subida.
Mudou uma entidade? A migração tem de acompanhar, senão o `validate` impede a subida, e é bom
que impeça.

**auth-api.** Escrever a função em `migrations/` e acrescentá-la **no fim** da lista de
`migrations/index.js`, com o número seguinte. O nome é a chave do histórico: renomear uma
migração que já rodou faz o Umzug achá-la nova. `tests/migracoes.test.js` confere a numeração
e a ordem das que dependem umas das outras.

**financeiro.**

```bash
cd services/financeiro
npm run migrate create nome-da-mudanca   # cria migrations/<timestamp>_nome-da-mudanca.sql
```

---

## O que a adoção corrigiu

Gerar o V1 a partir do que o Hibernate produz num banco vazio, e comparar o resultado com o
banco em uso, mostrou defeitos que o `ddl-auto: update` escondia:

| Achado | Efeito | Onde foi corrigido |
|---|---|---|
| Três tabelas do portaria (`blocos`, `areas_comuns`, `funcionario_jornadas`) não eram criadas num banco novo: a unicidade por condomínio citava `condominioId` sem aspas, e o Postgres recusava o `CREATE TABLE` | `blocos` e `areas_comuns` só existiam porque o `init-databases.sql` as criava; `funcionario_jornadas` simplesmente faltava, e a escala de funcionários quebrava num banco novo | portaria `V1` |
| O CHECK de `avisos.publico_alvo` não tinha `SINDICO`, valor que entrou no código em agosto | Publicar aviso só para o síndico falhava com SQLSTATE 23514 em todo banco criado antes disso | comunicacao `V3` |
| As migrações do financeiro não rodavam no Docker | Banco sem as tabelas mais novas; o erro só aparecia na primeira requisição | `financeiro/config/migracoes.js` |
| O auth-api rodava todas as migrações a cada boot e dependia do `init-databases.sql` para existir | Funcionava porque cada função checava a si mesma; um banco sem o init não subia | `auth-api/migrations/index.js` |
| Índices por condomínio, o índice único de `chave_unica` e a unicidade parcial de assinatura ativa só existiam em quem tinha rodado os scripts à mão | Consultas sem índice, e o financeiro podendo notificar duas vezes o mesmo evento | `V2` de cada serviço Java |
| O `init-databases.sql` inseria um administrador com senha fixa em todo banco novo | Conta conhecida em qualquer instalação | Removido. A conta inicial vem de `ADMIN_SEED_EMAIL`/`ADMIN_SEED_PASSWORD` |

---

## Como o V1 dos serviços Java foi gerado

Para que o V1 fosse o esquema que as entidades de fato pedem, e não uma transcrição à mão:

1. Cada serviço subiu com `ddl-auto: create` contra um banco vazio.
2. A estrutura foi lida do catálogo do Postgres (colunas, constraints, índices) e escrita como
   `CREATE TABLE IF NOT EXISTS`.
3. O script foi aplicado noutro banco vazio e comparado com o primeiro, coluna a coluna e
   constraint a constraint: zero diferenças nos quatro serviços. Aplicado de novo, não muda
   nada.
4. O mesmo script foi aplicado numa cópia do banco em uso. A única mudança foi a pretendida, o
   CHECK de `publico_alvo`.

Depois, com as imagens Docker desta versão, os seis serviços subiram em dois cenários
isolados, cada um com um Postgres próprio:

| Cenário | Resultado |
|---|---|
| Volume vazio, só com o `init-databases.sql` | Os seis criaram o próprio esquema e subiram, com o Hibernate validando. No `mora`, o segundo serviço a subir fez o baseline na versão 0 e rodou o V1 mesmo assim, que é o caso que motivou a configuração |
| Cópia do banco em uso (`pg_dumpall`) | Baseline na versão 0 nos Java, V1 sem efeito, `poll.data_hora_fim` criada, CHECK de `publico_alvo` corrigido; o auth-api registrou as 13 migrações e o financeiro estava em dia |
| Segunda subida | "No migration necessary" e "Banco em dia" |

O V2 de cada serviço traz o que o Hibernate não cria e que estava no banco em uso por scripts
manuais: índices, índices parciais, CHECKs e chaves com `ON DELETE CASCADE`.

---

## Histórico

Os scripts que eram aplicados à mão continuam no repositório, como registro de como o esquema
chegou até aqui, mas nenhuma ferramenta os executa:

- `services/comunicacao-service/src/main/resources/db/historico/` (fase5 a fase8)
- `services/portaria-service/src/main/resources/db/historico/` (fase3, fase4 e aprovação de reservas)
- `docker/historico/` (correções de constraint, separação por condomínio, ownership)

O que eles faziam e ainda vale está no V1 ou no V2 de cada serviço.
