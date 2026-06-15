
A comunicação entre o Banco 1 (`auth_db`) e o Banco 2 (`mora_db`) ocorre via mensageria. Nenhum serviço chama o outro diretamente — um banco publica um evento, o outro consome quando estiver pronto.

---

## Mecanismo central: pub/sub com filas

O modelo é **publish/subscribe**: quem produz um evento não sabe (nem precisa saber) quem vai consumi-lo.

**Na prática:**

1. Uma ação acontece no Banco 1 ou Banco 2
2. O serviço responsável **publica** uma mensagem (ex: `tenant.provisioned`)
3. O serviço do outro banco que **assina** aquele tópico recebe a mensagem e reage

---

## Fluxo: provisionamento de tenant

1. Super Admin insere o tenant em `platform.tenant` e chama `platform.create_identity_schema('tenant_xyz')`
2. Banco 1 publica `tenant.provisioned`
3. Banco 2 consome o evento e executa `platform_ops.create_tenant_schema('tenant_xyz')`, criando o schema operacional completo e inserindo o condomínio inicial

> Sem esse evento, o Banco 2 não tem conhecimento da existência do tenant.

---

## Fluxo: sincronização de usuários (`user_cache`)

Usuários residem exclusivamente no Banco 1. Para que o Banco 2 consiga referenciá-los em tabelas como `unit_occupant`, `fine` e `reservation`, existe a tabela `user_cache` em cada schema de tenant.

| Evento publicado pelo Banco 1 | Ação no Banco 2 |
|---|---|
| `user.created` | `INSERT` em `user_cache` |
| `user.updated` | `UPDATE` em `user_cache` |
| `user.deactivated` | `UPDATE user_cache SET is_active = false` |

Por isso, todas as colunas `*_user_id` no Banco 2 **não possuem FK formal** — elas referenciam `user_cache.user_id`, mas a consistência é eventual, não garantida por transação.

---

## Fluxo: vinculação de ocupante

Quando o **S2 (Property Service)** registra um ocupante em uma unidade no Banco 2:

1. Banco 2 publica `occupant.linked`
2. **S1 (Identity Service)** no Banco 1 consome o evento e cria o `user_role` correspondente, associando o usuário ao papel correto (`proprietario_residente`, `inquilino`, etc.) com o `condominium_id` e `unit_id` da unidade

O inverso ocorre com `occupant.unlinked`, que revoga o papel.

---

## Tabela de eventos

### Banco 1 → Banco 2

| Evento | Ação no Banco 2 |
|---|---|
| `tenant.provisioned` | Cria schema + condomínio inicial do tenant |
| `tenant.suspended` | Desativa todos os dados do tenant |
| `user.created` | `INSERT` em `user_cache` |
| `user.updated` | `UPDATE` em `user_cache` |
| `user.deactivated` | `UPDATE user_cache SET is_active = false` |

### Banco 2 → Banco 1

| Evento | Ação no Banco 1 |
|---|---|
| `occupant.linked` | S1 cria `user_role` na unidade |
| `occupant.unlinked` | S1 revoga `user_role` |
| `lease.activated` | S1 ajusta papel do inquilino |
| `lease.terminated` | S1 remove papel do inquilino |

---

## Consistência eventual

Este modelo implica uma **janela de tempo** entre um evento ser publicado e o outro banco processá-lo.

**Exemplo:** um usuário desativado no Banco 1 pode, por alguns milissegundos ou segundos (dependendo de carga), ainda aparecer como ativo no `user_cache` do Banco 2.

A aplicação foi projetada para tolerar esse comportamento — é exatamente por isso que não existem FKs cruzadas entre os bancos no schema.

---

## Implementação (RabbitMQ)

O transporte do pub/sub é o **RabbitMQ** (AMQP 0-9-1), adicionado ao `docker/docker-compose.yml` (serviço `rabbitmq`, painel em `http://localhost:15672`).

### Topologia

| Elemento | Valor | Observação |
|---|---|---|
| Exchange | `mora.events` | tipo `topic`, durável |
| Routing key | tipo do evento | ex.: `tenant.provisioned`, `user.created` |
| Fila (portaria) | `portaria.mora-events` | durável; bindings `tenant.*` e `user.*` |

### Formato da mensagem (envelope)

Toda mensagem publicada segue o mesmo envelope JSON, persistente (`delivery_mode=2`):

```json
{
  "eventId": "uuid-v4",
  "type": "tenant.provisioned",
  "payload": { "tenantId": 1, "schemaName": "tenant_xyz", "name": "...", "planId": 2 },
  "publishedAt": "2026-06-15T17:00:00.000Z"
}
```

O `eventId` é usado pelos consumidores para **idempotência** (deduplicação de reentregas).

### Lado publicador — auth-api (Node)

- `utils/messageBroker.js`: conexão `amqplib` com canal de confirmação, declaração do exchange e **reconexão automática**.
- `utils/eventPublisher.js`: `publicarEvento(tipo, payload)` monta o envelope e publica no broker. Se `RABBITMQ_URL` não estiver definido, usa **fallback em memória** (EventEmitter) — útil em dev/CI sem broker. A publicação com broker usa confirmação, então a falha de entrega reflete em `sucesso: false` (atende ao CA-03 do provisionamento de tenant).
- Conexão iniciada no boot (`server.js`) e encerrada no `SIGINT`/`SIGTERM`.

Variável de ambiente: `RABBITMQ_URL=amqp://mora:mora@rabbitmq:5672`.

### Lado consumidor — portaria-service (Spring AMQP)

- `messaging/RabbitMQConfig.java`: declara fila e bindings (`tenant.*`, `user.*`) e o conversor JSON.
- `messaging/MoraEventListener.java`: `@RabbitListener` que **deduplica** via inbox (`mensageria_evento_recebido`), despacha por tipo e confirma a mensagem mesmo em erro (evita _poison message_), deixando o registro como não processado para auditoria.
- **`user_cache`**: os eventos `user.created` / `user.updated` fazem _upsert_ na tabela `user_cache` (banco `mora`) e `user.deactivated` marca `is_active = false`. É assim que o Banco MORA referencia usuários sem FK cruzada, com consistência eventual.
- `tenant.provisioned` / `tenant.suspended` são registrados e logados (a criação física do schema do tenant fica como passo futuro).
- Pode ser desligado com `MORA_MESSAGING_ENABLED=false` (e fica desabilitado no profile de teste).

### Como rodar

```bash
# sobe todo o ambiente (inclui o broker)
docker compose -f docker/docker-compose.yml up -d

# painel do RabbitMQ: http://localhost:15672  (mora / mora)
```

Ao provisionar um tenant ou ativar/desativar um usuário no auth-api, a mensagem aparece na fila `portaria.mora-events` e é registrada na tabela `mensageria_evento_recebido` do banco `mora`.