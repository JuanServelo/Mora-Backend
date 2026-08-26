import { SERVICOS } from '../config/servicos.js';
import { buscar } from './httpClient.js';

/**
 * O auth-api é o dono de condomínios, usuários, convites e ocorrências, e
 * agrega os próprios dados em SQL. Aqui só consumimos o resultado.
 */
export function estatisticasPlataforma(authorization) {
  return buscar(`${SERVICOS.auth}/api/estatisticas/plataforma`, authorization);
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
