import { TIMEOUT_MS } from '../config/servicos.js';

/**
 * Busca em um serviço irmão repassando a identidade do usuário.
 *
 * Devolve `{ ok, dados, erro }` em vez de lançar: o dashboard compõe várias
 * fontes e uma indisponível deve virar um bloco vazio com aviso, não uma tela
 * de erro. Quem chama decide o que fazer com `ok: false`.
 */
export async function buscar(url, authorization) {
  try {
    const resposta = await fetch(url, {
      headers: {
        Accept: 'application/json',
        ...(authorization && { Authorization: authorization }),
      },
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });

    if (!resposta.ok) {
      return { ok: false, erro: `HTTP ${resposta.status}`, status: resposta.status };
    }

    return { ok: true, dados: await resposta.json() };
  } catch (err) {
    const motivo = err.name === 'TimeoutError' ? 'timeout' : err.message;
    console.warn(`[financeiro] Falha ao consultar ${url}: ${motivo}`);
    return { ok: false, erro: motivo };
  }
}
