# Decisões Arquiteturais — Mora

**Data:** 19/08/2026
**Contexto:** revisão da especificação do projeto para alinhar documentação, arquitetura e código.
**Base:** análise do código em [ARQUITETURA-E-FLUXOS.md](ARQUITETURA-E-FLUXOS.md), avaliação em
[relatorio-microservicos.md](relatorio-microservicos.md) e o documento
*BSI — Especificação do Projeto - Mora*.

Este arquivo registra **40 decisões** tomadas. Cada uma indica o que muda no código, no schema
e no documento. Serve como referência para a especificação e para o planejamento das sprints.

---

## Resumo executivo

| Tema | Antes | Depois |
|---|---|---|
| Cliente / tenant | Inexistente no código | `tenants` com `type` (`ADMINISTRADORA` \| `CONDOMINIO_DIRETO`) |
| Serviços | 5 | 8 (`vagas` removido, 4 novos) |
| RFs | 26 | 28 (+ Notificações, + Funcionários) |
| Perfis | 11 | 12 (+ `SUPER_ADMIN`) |
| ID de pessoa | 4 tipos incompatíveis | `UUID` único |
| Sincronização | Nenhuma | Outbox + polling HTTP |
| Histórias de usuário | Seção vazia | 28, com critérios de aceite |
| RNFs | Ausentes | Seção completa |

---

## Bloco A — Cliente, tenant e onboarding

### D01 — O cliente pode ser uma administradora ou um condomínio isolado
Existem duas trilhas comerciais: administradora com carteira de condomínios, ou síndico
contratando direto para um único condomínio.
**Consequência:** o modelo precisa comportar os dois sem duplicar regra de plano e cobrança.

### D02 — O cadastro do cliente é feito pelo Super Admin (venda comercial)
Não há página pública de cadastro nem trial automático. A venda ocorre fora do sistema e o
Super Admin registra o cliente.
**Consequência:** sem tela pública de signup; sem fluxo de aprovação; o `SUPER_ADMIN` passa a
ser perfil obrigatório.

### D03 — Isolamento por coluna, não por schema
O `schema_name` previsto no ER original é descartado. O isolamento é feito por coluna,
com filtro aplicado nas consultas.
**Consequência:** remove a necessidade de provisionar schema e replicar migrations por cliente.
Em compensação, o filtro por tenant/condomínio passa a ser obrigatório em todas as consultas.

### D04 — Os 28 RFs permanecem, com status explícito
A tabela de requisitos ganha as colunas **Status** (Implementado / Parcial / Planejado) e
**Sprint real**.
**Consequência:** o documento reflete a realidade sem perder o escopo do produto.

### D05 — `tenants` sempre existe, diferenciado por `type`
Todo condomínio pertence a um tenant. `tenants.type` assume `ADMINISTRADORA` ou
`CONDOMINIO_DIRETO`. Aproveita o `enum_tenants_type` já previsto no ER original.
**Consequência:** trilha única para plano, limite e cobrança. Não existe condomínio órfão.

### D33 — O cadastro do cliente cria tenant + assinatura + primeiro gestor
Em um único ato, o Super Admin cria o tenant, vincula o plano contratado e emite convite para o
`CONTRACTING_PROPERTY_MANAGER` ou `CONTRACTING_SYNDIC`. Os condomínios são cadastrados pelo
próprio cliente, respeitando `maxCondominiums`.
**Consequência:** o Super Admin não coleta dados de condomínio na venda.

### D36 — Inadimplência leva a somente leitura após tolerância
Vencido o prazo de tolerância, o tenant perde escrita e mantém consulta, com aviso em tela.
**Consequência:** `tenants.status` precisa de estado intermediário (ex.: `SUSPENDED_READONLY`);
o gateway passa a bloquear métodos de escrita para tenants nesse estado.

### D35 — `active_modules` é validado no gateway, por rota
O gateway consulta a assinatura em cache e bloqueia rotas de módulos não contratados.
**Consequência:** nenhum serviço de domínio precisa conhecer plano. Exige mapa rota → módulo.

---

## Bloco B — Identidade e perfis

### D24 — O primeiro Super Admin nasce por seed no boot
O `auth-api` cria a conta na primeira subida, com e-mail e senha vindos do `.env`, exigindo
troca de senha no primeiro login.
**Consequência:** duas variáveis novas de ambiente; o seed deve ser idempotente.

### D34 — Um usuário pertence a um único condomínio
`users` mantém `condominioId` e `perfil` diretamente, sem tabela de vínculos.
Perfis de nível tenant (`CONTRACTING_PROPERTY_MANAGER`, `CONTRACTING_SYNDIC`) ficam com
`tenantId` preenchido e `condominioId` nulo, operando sobre toda a carteira.
**Consequência aceita:** uma imobiliária que atua em vários condomínios precisará de uma conta
por condomínio, pois `REAL_ESTATE_AGENCY` é perfil de nível condomínio.

### D28 — `UUID` como identificador único de pessoa
`users.id` migra de `INTEGER` para `UUID`, e todos os serviços passam a referenciar esse valor.
**Consequência:** migração única no `auth_db`, com atualização de `registros_acesso`,
`reclamacoes`, `invites.usedByUserId` e `cadastradoPorId`. Elimina a tradução de tipo entre
bancos e resolve a fragmentação em 4 tipos.

### D40 — Colunas legadas de `users` são migradas e removidas
`role`, `bloco`, `apartamento` e `vaga` têm os dados migrados para `perfil` e `unidadeId`, e as
colunas são derrubadas.
**Consequência:** encerra o duplo modelo de autorização que hoje roda em paralelo.

---

## Bloco C — Integração entre serviços

### D09 — `user_cache` replicado no banco de cada serviço consumidor
Tabela com `userId`, `nome`, `email`, `perfil`, `condominioId`, `unidadeId` e `isActive`,
alimentada por evento do `auth-api`. Já estava prevista no ER original.
**Consequência:** os serviços de domínio fazem join local e continuam operando com o `auth-api`
indisponível. Exige tratar dado momentaneamente desatualizado.

### D10 — Sincronização por outbox + polling HTTP
Usa a tabela `mensageria_evento_recebido` do ER original, com entrega por HTTP e
reprocessamento controlado por flag. Sem broker novo no `docker-compose`.
**Consequência:** entrega eventualmente consistente, com retentativa. Caminho de evolução
natural para RabbitMQ, se o volume justificar.

### D11 — `vagas-service` é fundido no `portaria-service`
Aluguel e disponibilidade de vaga migram para o `portaria-service`, que já é dono de
`vagas_estacionamento`. O banco `vagas_db` é descontinuado.
**Consequência:** elimina 4 tabelas espelho (`blocos`, `apartamentos`, `moradores`,
`vagas_estacionamento`) e um banco inteiro.

### D06 — `tenantId` fica em `condominios` e `users`; `condominioId` vai para todas as tabelas de domínio
Consultas de carteira agregam por condomínio.
**Consequência:** evita redundância entre duas colunas de tenancy e mantém o filtro simples.

---

## Bloco D — Topologia de serviços

### D08 — RF-8 (Entradas e Saídas) unifica no `portaria-service`
O `auth-api` perde `registros_acesso` e as rotas `/api/portaria`.
**Consequência:** encerra a implementação dupla de controle de acesso. Exige migrar os dados e
depende do `user_cache` (D09).

### D13 — Novo `financeiro-service`
Serviço Java dedicado, com banco próprio, para faturas, multas, taxas e prestação de contas.

### D17 — Novo `comunicacao-service`
Reúne chat (RF-25), avisos (RF-12), base de conhecimento (RF-10) e notificações (RF-27),
retirando esses domínios do `portaria-service`.
**Consequência:** o `portaria-service` cai de 19 para cerca de 14 controllers.

### D27 — Novo `ocorrencias-service`
Reclamações (RF-22) e ordens de serviço (RF-24). `Reclamacao` sai do `auth-api`.

### D15 — Novo `analytics-service`, com ETL agendado
Job consolida dados dos bancos em tabelas de indicadores, como o mockup já promete
("ETL executado diariamente às 02h00").
**Consequência:** viabiliza o Painel da Carteira e séries históricas, que não são obtíveis
consultando os bancos separadamente.

### D14 — `Assinatura` fica no `plan-service`
O serviço passa a ser dono do catálogo **e** do contrato: `Assinatura` com `tenantId`,
`planId`, vigência e status.
**Consequência:** resolve a lacuna atual em que os limites do plano não são aplicáveis por não
existir vínculo entre condomínio e plano.

### Topologia resultante

| Serviço | Situação | Domínio |
|---|---|---|
| `auth-api` | Existente | Identidade, tenants, condomínios, convites, usuários |
| `portaria-service` | Existente, reduzido | Estrutura física, pessoas, acessos, veículos, vagas, chaves, entregas, áreas comuns e reservas |
| `meeting-service` | Existente | Assembleias, atas, votações |
| `plan-service` | Existente, ampliado | Planos e assinaturas |
| ~~`vagas-service`~~ | **Removido** | Fundido no portaria (D11) |
| `financeiro-service` | **Novo** | Faturas, multas, taxas, prestação de contas |
| `comunicacao-service` | **Novo** | Chat, avisos, base de conhecimento, notificações |
| `ocorrencias-service` | **Novo** | Reclamações e ordens de serviço |
| `analytics-service` | **Novo** | ETL e indicadores |

---

## Bloco E — Escopo dos requisitos

### D07 — O bloco financeiro é especificado agora, marcado como Planejado
RF-14, 15, 16, 17 e 21 ganham fluxos, entidades e regras nesta revisão, ainda sem código.
**Consequência:** o Painel Financeiro passa a ter fonte de dados definida.

### D12 — O financeiro cobre os dois fluxos de dinheiro
Plataforma fatura o tenant pela assinatura, e o condomínio fatura o morador.

### D29 — Contas separadas: cada condomínio recebe na sua, a plataforma na dela
O dinheiro do condomínio nunca transita pela plataforma.
**Consequência:** reduz drasticamente a responsabilidade legal e fiscal do Mora. Cada
condomínio cadastra a própria conta de recebimento.

### D26 — PIX e boleto integrados via gateway
**Consequência:** dependência externa, webhook de confirmação e rotina de conciliação. É a
decisão de maior custo técnico do conjunto — avaliar como candidata a Planejado.

### D25 — O rateio da taxa condominial é configurável por condomínio
Valor fixo por unidade ou rateio por fração ideal, definido no RF-21.
**Consequência:** `apartamentos` ganha o campo de fração ideal.

### D16 — RF-25 é chat entre usuários
Mensagens diretas entre morador, síndico e portaria, com histórico.

### D18 — Criado o RF-27 — Notificações
Central de notificações disparada por eventos (encomenda recebida, fatura vencendo, assembleia
marcada, visitante autorizado), aproveitando o outbox de D10.
**Motivo:** com o RF-25 definido como chat, nenhum requisito cobria notificação automática.

### D23 — Criado o RF-28 — Gestão de Funcionários e Turnos
Regulariza `funcionarios`, `turnos`, `turno_entradas` e `turno_saidas`, que existem no código
e não constavam em nenhum RF.

### D19 — RF-12 passa a registrar leitura por usuário
Tabela `aviso_leitura` (aviso, usuário, data).
**Consequência:** sustenta o KPI "leitura de comunicados" do Painel Operacional, que hoje não é
computável, e serve como comprovação de ciência de regras.

### D20 — RF-19 é especificado por completo
Reserva com antecedência mínima, bloqueio de conflito, limite por unidade, aprovação do síndico
e taxa que gera fatura. Cria a tabela `reservas`, prevista no ER original e ausente do código.
**Consequência:** sustenta o KPI "receita de reservas".

### D37 — RF-18: morador pré-autoriza com validade, guarita vê a lista do dia
### D38 — RF-11: destinatário da encomenda é usuário cadastrado, com notificação
**Consequência:** substitui os campos de texto livre atuais e permite histórico por pessoa.

### D39 — RF-24: OS nasce de ocorrência ou avulsa, com fornecedor, prazo e status
**Consequência:** sustenta os KPIs "OS no prazo" e "OS críticas atrasadas". Cobre também
manutenção preventiva.

### D21 — RF-23: um voto por unidade
Cada apartamento vale um voto, exercido pelo proprietário ou por quem ele indicar.
**Consequência:** `tb_poll_vote` ganha constraint de unicidade por unidade, não por usuário.

### D22 — `Carro` é removido; `Veiculo` permanece
`Veiculo` é mais completo (tem `categoria` e vínculo com `Vaga`).
**Consequência:** encerra o risco de a mesma placa ter status contraditório em duas tabelas.

---

## Bloco F — Documentação

### D30 — As 28 histórias de usuário serão escritas com critérios de aceite
Formato Como/Quero/Para, com critérios em Dado/Quando/Então.
**Motivo:** a seção existe no documento e está vazia — é o único artefato que atribuiria
definição de pronto aos requisitos.

### D31 — Seção completa de requisitos não-funcionais
Segurança, LGPD, desempenho, disponibilidade e observabilidade.
**Motivo:** o sistema guarda CPF, histórico de movimentação de pessoas e passará a tratar dado
de pagamento.

### D32 — Fonte em Markdown no repositório, com exportação para `.docx`
Documentação versionada em `docs/`, evoluindo por pull request; o arquivo da PUCPR é gerado a
partir dela.

---

## Decisões que aumentam escopo — atenção ao planejamento

Quatro decisões concentram a maior parte do esforço novo. Se o prazo apertar, são as primeiras
candidatas a permanecer como **Planejado** em vez de implementadas:

| Decisão | Esforço | Observação |
|---|---|---|
| D26 — PIX e boleto integrados | Alto | Dependência externa, webhook, conciliação |
| D15 — `analytics-service` com ETL | Alto | Serviço novo + job + modelagem de indicadores |
| D13 — `financeiro-service` | Alto | Domínio inteiro do zero |
| D28 — Migração de `INTEGER` para `UUID` | Médio | Toca todos os serviços de uma vez |

Em contrapartida, três decisões **reduzem** trabalho e risco: D11 (remove um serviço e um
banco), D22 (remove uma entidade duplicada) e D40 (remove o duplo modelo de autorização).

---

## Rastreamento

| # | Decisão | Bloco |
|---|---|---|
| D01 | Cliente pode ser administradora ou condomínio isolado | A |
| D02 | Cadastro do cliente pelo Super Admin | A |
| D03 | Isolamento por coluna, não por schema | A |
| D04 | 28 RFs com status explícito | A |
| D05 | `tenants` sempre existe, com `type` | A |
| D06 | `tenantId` em `condominios`/`users`, `condominioId` no domínio | C |
| D07 | Financeiro especificado agora como Planejado | E |
| D08 | RF-8 unifica no `portaria-service` | D |
| D09 | `user_cache` replicado | C |
| D10 | Outbox + polling HTTP | C |
| D11 | `vagas-service` fundido no portaria | C |
| D12 | Financeiro cobre os dois fluxos | E |
| D13 | Novo `financeiro-service` | D |
| D14 | `Assinatura` no `plan-service` | D |
| D15 | Novo `analytics-service` com ETL | D |
| D16 | RF-25 é chat entre usuários | E |
| D17 | Novo `comunicacao-service` | D |
| D18 | Criado RF-27 — Notificações | E |
| D19 | RF-12 com registro de leitura | E |
| D20 | RF-19 completo, com taxa | E |
| D21 | RF-23: um voto por unidade | E |
| D22 | `Carro` removido, `Veiculo` mantido | E |
| D23 | Criado RF-28 — Funcionários e Turnos | E |
| D24 | Super Admin por seed no boot | B |
| D25 | Rateio configurável por condomínio | E |
| D26 | PIX e boleto integrados | E |
| D27 | Novo `ocorrencias-service` | D |
| D28 | `UUID` como ID de pessoa | B |
| D29 | Contas de recebimento separadas | E |
| D30 | 28 histórias com critérios de aceite | F |
| D31 | Seção completa de RNFs | F |
| D32 | Markdown no repo, exportando `.docx` | F |
| D33 | Cadastro cria tenant + assinatura + gestor | A |
| D34 | Um condomínio por usuário | B |
| D35 | `active_modules` validado no gateway | A |
| D36 | Inadimplência leva a somente leitura | A |
| D37 | RF-18: pré-autorização com validade | E |
| D38 | RF-11: destinatário é usuário cadastrado | E |
| D39 | RF-24: OS de ocorrência ou avulsa | E |
| D40 | Colunas legadas migradas e removidas | B |
