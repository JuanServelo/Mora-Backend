/**
 * Conexão com o broker de mensageria (RabbitMQ via AMQP 0-9-1).
 *
 * Implementa o transporte real do pub/sub descrito em docs/assincrona.md:
 * o auth-api (Banco AUTH) publica eventos de domínio em um topic exchange
 * (`mora.events`) usando a routing key = tipo do evento (ex.: `tenant.provisioned`).
 * Os serviços do Banco MORA assinam os tópicos de interesse e consomem de forma
 * assíncrona.
 *
 * O módulo é tolerante a falhas: se o broker não estiver configurado
 * (sem `RABBITMQ_URL`) ou estiver temporariamente indisponível, a aplicação
 * continua funcionando — quem publica decide o que fazer com a falha
 * (ver utils/eventPublisher.js).
 */
import amqp from 'amqplib';

export const EXCHANGE = 'mora.events';
const EXCHANGE_TYPE = 'topic';
const RECONNECT_DELAY_MS = 5000;

let connection = null;
let channel = null;
let conectando = false;
let encerrando = false;

/** Indica se há um canal pronto para publicar. */
export function brokerDisponivel() {
  return channel !== null;
}

/** Indica se a mensageria está habilitada (variável de ambiente presente). */
export function brokerConfigurado() {
  return Boolean(process.env.RABBITMQ_URL);
}

function agendarReconexao() {
  if (encerrando || conectando) return;
  setTimeout(() => {
    conectarBroker().catch(() => {
      /* erro já logado em conectarBroker */
    });
  }, RECONNECT_DELAY_MS);
}

/**
 * Estabelece conexão com o RabbitMQ e declara o exchange (idempotente).
 * Reconecta automaticamente em caso de queda. Não lança em caso de falha:
 * apenas loga e reagenda a tentativa.
 */
export async function conectarBroker() {
  if (!brokerConfigurado()) {
    console.warn('[broker] RABBITMQ_URL não definido — mensageria via broker desabilitada (fallback em memória)');
    return null;
  }
  if (channel || conectando) return channel;

  conectando = true;
  try {
    connection = await amqp.connect(process.env.RABBITMQ_URL);

    connection.on('error', (err) => {
      console.error('[broker] erro na conexão:', err.message);
    });
    connection.on('close', () => {
      if (encerrando) return;
      console.warn('[broker] conexão encerrada — tentando reconectar...');
      channel = null;
      connection = null;
      agendarReconexao();
    });

    channel = await connection.createConfirmChannel();
    await channel.assertExchange(EXCHANGE, EXCHANGE_TYPE, { durable: true });

    console.log(`[broker] conectado ao RabbitMQ e exchange '${EXCHANGE}' pronto`);
    return channel;
  } catch (err) {
    console.error('[broker] falha ao conectar:', err.message);
    channel = null;
    connection = null;
    agendarReconexao();
    return null;
  } finally {
    conectando = false;
  }
}

/**
 * Publica uma mensagem persistente no exchange `mora.events`.
 * @param {string} routingKey - tipo do evento (ex.: 'tenant.provisioned')
 * @param {object} envelope - corpo completo da mensagem (eventId, type, payload, publishedAt)
 * @returns {Promise<boolean>} true se a publicação foi confirmada pelo broker
 */
export async function publicarNoBroker(routingKey, envelope) {
  if (!channel) {
    await conectarBroker();
  }
  if (!channel) {
    throw new Error('Broker indisponível');
  }

  const conteudo = Buffer.from(JSON.stringify(envelope));
  return new Promise((resolve, reject) => {
    channel.publish(
      EXCHANGE,
      routingKey,
      conteudo,
      {
        persistent: true,
        contentType: 'application/json',
        messageId: envelope.eventId,
        timestamp: Date.now(),
        type: routingKey,
      },
      (err) => {
        if (err) reject(err);
        else resolve(true);
      },
    );
  });
}

/** Encerra a conexão de forma limpa (graceful shutdown). */
export async function fecharBroker() {
  encerrando = true;
  try {
    if (channel) await channel.close();
    if (connection) await connection.close();
    console.log('[broker] conexão encerrada com sucesso');
  } catch (err) {
    console.error('[broker] erro ao encerrar conexão:', err.message);
  } finally {
    channel = null;
    connection = null;
  }
}
