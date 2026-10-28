import { SERVICOS } from '../config/servicos.js';
import { buscar } from './httpClient.js';

const BASE = () => `${SERVICOS.financeiro}/api/financeiro`;

/** Situação das faturas de uma competência ("YYYY-MM"), agregada no banco do financeiro. */
export function kpisDaCompetencia(competencia, authorization) {
  return buscar(`${BASE()}/admin/faturas/kpis?competencia=${encodeURIComponent(competencia)}`, authorization);
}

/**
 * Situação das faturas mês a mês, nos últimos `meses` (3, 6 ou 12): uma
 * consulta agrupada no financeiro, com uma linha por mês, inclusive os vazios.
 */
export function kpisPorMes(meses, authorization) {
  return buscar(`${BASE()}/admin/faturas/kpis/por-mes?meses=${encodeURIComponent(meses)}`, authorization);
}

/** Multas aplicadas, canceladas e valor por mês ("YYYY-MM"), agregadas no banco do financeiro. */
export function multasPorMes(meses, authorization) {
  return buscar(`${BASE()}/admin/multas/por-mes?meses=${encodeURIComponent(meses)}`, authorization);
}
