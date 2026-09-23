import { SERVICOS } from '../config/servicos.js';
import { buscar } from './httpClient.js';

/**
 * O portaria-service ainda não expõe endpoints de contagem — só listagens com
 * `?condominioId=`. Contamos aqui a partir da lista.
 *
 * Funciona bem no porte atual; se o volume crescer, o caminho é adicionar
 * endpoints de contagem no portaria e trocar estas chamadas, sem que o contrato
 * do dashboard mude.
 */
async function contar(caminho, condominioId, authorization) {
  const url = condominioId
    ? `${SERVICOS.portaria}${caminho}?condominioId=${encodeURIComponent(condominioId)}`
    : `${SERVICOS.portaria}${caminho}`;

  const r = await buscar(url, authorization);
  if (!r.ok) return { ok: false, erro: r.erro };
  return { ok: true, total: Array.isArray(r.dados) ? r.dados.length : 0, itens: r.dados };
}

/**
 * Estrutura física de um condomínio: blocos, apartamentos e áreas comuns.
 * Devolve `{ ok: false }` se o portaria estiver indisponível — quem chama
 * decide se degrada ou falha.
 */
export async function estruturaDoCondominio(condominioId, authorization) {
  const [blocos, apartamentos, areas] = await Promise.all([
    contar('/blocos', condominioId, authorization),
    contar('/apartamentos', condominioId, authorization),
    contar('/areas-comuns', condominioId, authorization),
  ]);

  if (!blocos.ok && !apartamentos.ok && !areas.ok) {
    return { ok: false, erro: blocos.erro };
  }

  return {
    ok: true,
    dados: {
      blocos: blocos.ok ? blocos.total : null,
      apartamentos: apartamentos.ok ? apartamentos.total : null,
      areasComuns: areas.ok ? areas.total : null,
    },
  };
}
