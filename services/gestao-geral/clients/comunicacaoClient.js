import { SERVICOS } from '../config/servicos.js';
import { buscar } from './httpClient.js';

// O comunicacao-service recorta tudo pelo condomínio do token repassado.

/** Avisos do condomínio, inclusive rascunhos — é a visão da gestão. */
export function listarAvisos(authorization) {
  return buscar(`${SERVICOS.comunicacao}/avisos`, authorization);
}

/** Quantas confirmações de leitura um aviso recebeu. */
export function leiturasDoAviso(avisoId, authorization) {
  return buscar(`${SERVICOS.comunicacao}/avisos/${encodeURIComponent(avisoId)}/leituras`, authorization);
}

/** Conversas que a gestão enxerga, com a contagem de não lidas. */
export function listarConversas(authorization) {
  return buscar(`${SERVICOS.comunicacao}/conversas`, authorization);
}
