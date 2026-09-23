import { SERVICOS } from '../config/servicos.js';
import { buscar } from './httpClient.js';

/**
 * O auth-api é o dono de condomínios, usuários, convites e ocorrências, e
 * agrega os próprios dados em SQL. Aqui só consumimos o resultado.
 */
export function estatisticasPlataforma(authorization, filtros = {}) {
  // Os filtros viajam até o dono do dado, que agrega já recortado. O painel
  // nunca recebe a base inteira para peneirar.
  const params = new URLSearchParams();
  if (filtros.meses) params.set('meses', filtros.meses);
  if (filtros.status && filtros.status !== 'todos') params.set('status', filtros.status);
  const query = params.toString();

  return buscar(
    `${SERVICOS.auth}/api/estatisticas/plataforma${query ? `?${query}` : ''}`,
    authorization,
  );
}

export function resumoCondominio(id, authorization) {
  return buscar(
    `${SERVICOS.auth}/api/estatisticas/condominios/${encodeURIComponent(id)}`,
    authorization,
  );
}

export function listarCondominios(authorization) {
  return buscar(`${SERVICOS.auth}/api/condominios`, authorization);
}
