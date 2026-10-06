import { SERVICOS } from '../config/servicos.js';
import { buscar } from './httpClient.js';

const BASE = () => `${SERVICOS.financeiro}/api/financeiro`;

/** Situação das faturas de uma competência ("YYYY-MM"), agregada no banco do financeiro. */
export function kpisDaCompetencia(competencia, authorization) {
  return buscar(`${BASE()}/admin/faturas/kpis?competencia=${encodeURIComponent(competencia)}`, authorization);
}

/** Multas do condomínio de quem pede. */
export function listarMultas(authorization) {
  return buscar(`${BASE()}/admin/multas`, authorization);
}
