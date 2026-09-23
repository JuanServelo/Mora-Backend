import { SERVICOS } from '../config/servicos.js';
import { buscar } from '../utils/httpClient.js';

/**
 * Unidades do condomínio, que é sobre o que se cobra.
 *
 * O `ApartamentoResponseDTO` do portaria-service não devolve `condominioId` —
 * o filtro acontece lá, pelo parâmetro. Consequência: se alguma unidade tiver
 * `condominioId` nulo no banco do portaria, ela não aparece aqui e **nunca é
 * faturada**, sem erro nenhum. Por isso o preflight do fechamento mostra a
 * contagem de unidades para conferência humana antes de emitir.
 */
export async function listarUnidades(condominioId, authorization) {
  const url = `${SERVICOS.portaria}/apartamentos?condominioId=${encodeURIComponent(condominioId)}`;
  const r = await buscar(url, authorization);
  if (!r.ok) return r;

  const unidades = (r.dados || []).map((a) => ({
    id: a.id,
    numero: a.numero,
    andar: a.andar,
    blocoId: a.blocoId,
    blocoNome: a.blocoNome,
    area: a.areaMxComTotal ?? null,
  }));

  return { ok: true, dados: unidades };
}
