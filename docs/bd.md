# Como Funciona a Estrutura de Dados
## Dois bancos de dados separados

O sistema usa dois bancos PostgreSQL distintos, comunicados por eventos assíncronos 

- **Banco 1 (`auth_db`)**: responsável por autenticação e plataforma. Contém os serviços S0 (Platform) e S1 (Identity).
- **Banco 2 (`mora_db`)**: responsável pelas operações condominiais. Contém todos os outros serviços (S2 a S11).

Essa separação evita que dados de autenticação fiquem misturados com dados operacionais de cada condomínio.

***

## Banco 1 - Autenticação e Plataforma

O schema `platform` é **compartilhado** (não isolado por tenant) e guarda três tabelas principais:

- `platform.plan` — catálogo de planos SaaS (ex.: "Starter", com limite de condomínios e usuários)
- `platform.tenant` — os clientes que contratam o Mora (administradoras ou síndicos)

O S1 (Identity Service) cria um **schema por tenant** dinamicamente via a função `platform.create_identity_schema()`, contendo as tabelas `user`, `role` e `user_role`.  O ponto crítico aqui: `user_role.condominium_id` e `user_role.unit_id` são UUIDs **sem FK formal**, pois os dados de condomínios vivem no Banco 2, a integridade é garantida por eventos assíncronos.

***

## Banco 2 - Operações Condominiais

Ao receber o evento `tenant.provisioned` do Banco 1, a função `platform_ops.create_tenant_schema()` cria um **schema isolado por tenant** com todas as tabelas operacionais.  Os serviços cobertos são:


| Serviço | Tabelas principais | RFs cobertas (desatualizado) |
| :-- | :-- | :-- |
| S2 Property | `condominium`, `building`, `unit`, `unit_occupant`, `lease_contract` | RF03, RF04, RF05 |
| S3 Access | `visitor`, `access_log`, `package` | RF06, RF07, RF20 |
| S4 Amenity | `common_area`, `reservation`, `key_control`, `parking_spot`, `parking_rental` | RF08, RF09, RF10 |
| S5 Complaint | `complaint`, `complaint_interaction` | RF17 |
| S6 Financial | `tax_config`, `invoice`, `invoice_item`, `fine`, `account_statement` | RF11–RF14 |
| S7 Governance | `assembly`, `assembly_attendance`, `minutes`, `poll`, `vote`, `service_order` | RF15, RF16, RF18 |
| S8 Chat | `message_channel`, `message`, `channel_presence` | RF21 |
| S9 Content | `announcement`, `announcement_view`, `kb_article` | RF19, RF22 |
| S11 Notification | `notification`, `audit_log` | transversal |


***

## O papel do `user_cache`

Como os usuários vivem no Banco 1 e as operações no Banco 2, há uma tabela `user_cache` em cada schema de tenant no Banco 2.  Ela é alimentada por eventos: `user.created`, `user.updated` e `user.deactivated`. Todas as colunas `*_user_id` do Banco 2 referenciam `user_cache.user_id` sem FK formal — a consistência é eventual, não transacional.

***

## Fluxo de eventos entre os bancos

Os bancos se comunicam de forma assíncrona pelos seguintes eventos:

**Banco 1 → Banco 2:**

- `tenant.provisioned` → cria schema + condomínio inicial
- `tenant.suspended` → desativa todos os dados do tenant
- `user.created/updated/deactivated` → sincroniza `user_cache`

**Banco 2 → Banco 1:**

- `occupant.linked/unlinked` → S1 cria ou revoga `user_role`
- `lease.activated/terminated` → S1 ajusta o papel do inquilino

***

## Schema `analytics` (S10 — Reporting)

Existe um schema `analytics` **compartilhado** (fora dos schemas de tenant) alimentado por um ETL que lê de todos os schemas operacionais.  Ele contém tabelas como `daily_financial_summary`, `monthly_occupancy`, `reservation_usage`, `complaint_sla`, usadas para dashboards (RF23) e relatórios analíticos (RF25).
<span style="display:none"></span>


