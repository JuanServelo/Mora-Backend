import { SERVICOS } from '../config/servicos.js';
import { buscar } from './httpClient.js';

// O comunicacao-service recorta tudo pelo condomínio do token repassado.

/**
 * Avisos vigentes e publicados do condomínio, cada um com o total de
 * confirmações de leitura — contadas numa consulta agrupada no comunicacao.
 * Antes era uma chamada por aviso.
 */
export function panoramaDeLeitura(authorization) {
  return buscar(`${SERVICOS.comunicacao}/avisos/leituras`, authorization);
}

/** Conversas que a gestão enxerga, com a contagem de não lidas. */
export function listarConversas(authorization) {
  return buscar(`${SERVICOS.comunicacao}/conversas`, authorization);
}
