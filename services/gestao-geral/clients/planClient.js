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
 * Todas as assinaturas da plataforma, para agregar receita.
 *
 * Uma chamada só em vez de uma por condomínio: com dezenas de clientes, o
 * caminho por condomínio faria o painel esperar dezenas de idas ao plan-service.
 */
export async function listarAssinaturas(authorization) {
  const r = await buscar(`${SERVICOS.plan}/api/assinaturas`, authorization);
  if (!r.ok) return r;
  return { ok: true, dados: Array.isArray(r.dados) ? r.dados : [] };
}
