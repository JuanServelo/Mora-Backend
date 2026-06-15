/**
 * Publicador de eventos de domínio (pub/sub Banco AUTH → Banco MORA).
 *
 * Conforme docs/assincrona.md, o Banco 1 (auth_db) publica eventos
 * (ex.: `tenant.provisioned`) e o Banco 2 (mora_db) consome de forma assíncrona.
 *
 * Transporte:
 *  - Quando `RABBITMQ_URL` está configurado, os eventos são publicados em um
 *    topic exchange do RabbitMQ (ver utils/messageBroker.js). A publicação usa
 *    confirmação do broker, então o sucesso reflete a entrega real.
 *  - Quando o broker não está configurado (dev/CI sem broker), os eventos são
 *    apenas registrados em memória e emitidos via EventEmitter local — útil para
 *    desenvolvimento e testes, sem quebrar o fluxo.
 */
import { randomUUID } from 'crypto';
import { EventEmitter } from 'events';
import {
  publicarNoBroker,
  brokerConfigurado,
} from './messageBroker.js';

export const domainEvents = new EventEmitter();

const historico = [];

/**
 * Publica um evento de domínio.
 * @param {string} tipo - nome do evento (ex.: 'tenant.provisioned')
 * @param {object} payload - dados do evento
 * @returns {Promise<{ sucesso: boolean, evento?: object, erro?: string }>}
 */
export async function publicarEvento(tipo, payload = {}) {
  const envelope = {
    eventId: randomUUID(),
    type: tipo,
    payload,
    publishedAt: new Date().toISOString(),
  };

  // Assinantes locais e histórico continuam funcionando em qualquer cenário.
  historico.push({ tipo, payload, publicadoEm: envelope.publishedAt });
  domainEvents.emit(tipo, payload);

  if (!brokerConfigurado()) {
    console.log(`[event] publicado (memória): ${tipo}`, JSON.stringify(payload));
    return { sucesso: true, evento: envelope };
  }

  try {
    await publicarNoBroker(tipo, envelope);
    console.log(`[event] publicado (broker): ${tipo} [${envelope.eventId}]`);
    return { sucesso: true, evento: envelope };
  } catch (err) {
    console.error(`[event] falha ao publicar ${tipo} no broker:`, err.message);
    return { sucesso: false, erro: err.message };
  }
}

/** Histórico de eventos publicados (uso interno / testes). */
export function listarEventosPublicados() {
  return [...historico];
}
