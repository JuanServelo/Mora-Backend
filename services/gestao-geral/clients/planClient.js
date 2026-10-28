import { SERVICOS } from '../config/servicos.js';
import { buscar } from './httpClient.js';

/**
 * Assinatura vigente de um condomínio.
 *
 * A resposta do plan-service já traz os limites do plano resolvidos, então uma
 * chamada basta para saber o que o condomínio contratou.
 */
export async function assinaturaDoCondominio(condominioId, authorization) {
  const r = await buscar(
    `${SERVICOS.plan}/api/assinaturas/condominio/${encodeURIComponent(condominioId)}`,
    authorization,
  );

  // 404 e 400 não são falha de infraestrutura: o condomínio simplesmente não
  // tem assinatura ainda. Distinguir isso evita marcar a fonte como fora do ar.
  if (!r.ok) {
    return r.status === 404 || r.status === 400
      ? { ok: true, dados: null }
      : { ok: false, erro: r.erro };
  }

  return { ok: true, dados: r.dados };
}

/**
 * Painel de receita da plataforma, já agregado no banco do plan-service pela
 * função `receita_plataforma`: indicadores, receita por plano, evolução mensal
 * e um resumo por cliente. Antes vinham todas as assinaturas para somar aqui.
 */
export function receitaDaPlataforma(filtros, authorization) {
  const params = new URLSearchParams();
  if (filtros.meses) params.set('meses', filtros.meses);
  if (filtros.plano && filtros.plano !== 'todos') params.set('plano', filtros.plano);
  const query = params.toString();
  return buscar(`${SERVICOS.plan}/api/assinaturas/receita${query ? `?${query}` : ''}`, authorization);
}
