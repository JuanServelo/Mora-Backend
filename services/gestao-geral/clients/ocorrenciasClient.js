import { SERVICOS } from '../config/servicos.js';
import { buscar } from './httpClient.js';

/**
 * O ocorrencias-service é o dono de reclamações e ordens de serviço (D27), e
 * agrega os próprios dados em SQL. Aqui só consumimos o resultado. Ele recorta
 * pelo condomínio do token repassado.
 */

/**
 * Totais das ocorrências do condomínio de quem pede: situação atual e as
 * séries dos últimos `meses`.
 */
export function estatisticasReclamacoes(meses, authorization) {
  return buscar(
    `${SERVICOS.ocorrencias}/reclamacoes/estatisticas?meses=${encodeURIComponent(meses)}`,
    authorization,
  );
}

/**
 * Ocorrências por status, para o painel da plataforma (só Admin Geral).
 *
 * `condominioIds` nulo = todos; lista vazia = nenhum condomínio passou no
 * filtro, e a resposta é zero — por isso o parâmetro vai mesmo vazio.
 */
export function contagemReclamacoes(condominioIds, authorization) {
  const query = condominioIds === null || condominioIds === undefined
    ? ''
    : `?condominioIds=${encodeURIComponent(condominioIds.join(','))}`;
  return buscar(`${SERVICOS.ocorrencias}/reclamacoes/contagem${query}`, authorization);
}

/** Indicadores das ordens de serviço: atrasadas, críticas atrasadas, no prazo. */
export function indicadoresOrdensServico(authorization) {
  return buscar(`${SERVICOS.ocorrencias}/ordens-servico/estatisticas`, authorization);
}
