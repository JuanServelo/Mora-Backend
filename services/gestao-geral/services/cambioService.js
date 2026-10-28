/**
 * Cotação do dólar, para a tela em inglês mostrar o valor aproximado em US$.
 *
 * Só exibição: cobranças, boletos e PIX continuam em real, e nenhum valor
 * gravado passa por aqui. A fonte é a PTAX do Banco Central, pública e sem
 * chave. Ela não publica em fim de semana e feriado, por isso a consulta pega
 * a cotação mais recente dos últimos dias, e não a de hoje.
 *
 * A cotação muda uma vez por dia útil; guardar por algumas horas evita uma ida
 * ao Banco Central a cada tela aberta. Se ele não responder, vale a última
 * cotação conhecida, mesmo vencida: um valor de referência de ontem é melhor
 * que nenhum.
 */

const URL_PTAX = process.env.PTAX_URL
  || 'https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/CotacaoDolarPeriodo(dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)';

export const VALIDADE_MS = 6 * 60 * 60 * 1000;
const TIMEOUT_MS = 5000;
/** Cobre fim de semana prolongado por feriado. */
const DIAS_PARA_TRAS = 7;

let cache = null;

/** Só para os testes começarem sem cotação guardada. */
export function limparCache() {
  cache = null;
}

/** A PTAX recebe as datas como 'MM-DD-AAAA', no fuso de Brasília. */
function dataPtax(instante) {
  const [ano, mes, dia] = new Date(instante)
    .toLocaleDateString('sv-SE', { timeZone: 'America/Sao_Paulo' })
    .split('-');
  return `'${mes}-${dia}-${ano}'`;
}

function urlConsulta(agora) {
  const inicio = agora - DIAS_PARA_TRAS * 24 * 60 * 60 * 1000;
  const parametros = {
    '@dataInicial': dataPtax(inicio),
    '@dataFinalCotacao': dataPtax(agora),
    $format: 'json',
    $orderby: 'dataHoraCotacao desc',
    $top: '1',
  };
  // Sem URLSearchParams: ele troca o espaço por '+', e a PTAX recusa com 400.
  const query = Object.entries(parametros)
    .map(([nome, valor]) => `${encodeURIComponent(nome)}=${encodeURIComponent(valor)}`)
    .join('&');
  return `${URL_PTAX}?${query}`;
}

/** A cotação de venda mais recente, ou `null` se a resposta não trouxer uma. */
async function consultarPtax(agora) {
  const resposta = await fetch(urlConsulta(agora), {
    headers: { Accept: 'application/json' },
    signal: AbortSignal.timeout(TIMEOUT_MS),
  });
  if (!resposta.ok) throw new Error(`HTTP ${resposta.status}`);

  const [ultima] = (await resposta.json())?.value ?? [];
  const valor = Number(ultima?.cotacaoVenda);
  if (!ultima || !(valor > 0)) return null;

  return { reaisPorDolar: valor, dataCotacao: String(ultima.dataHoraCotacao).slice(0, 10) };
}

/**
 * `{ reaisPorDolar, dataCotacao }`, ou `{ erro, status, mensagem }` quando não
 * há cotação nenhuma para devolver.
 */
export async function obterCotacaoDolar(agora = Date.now()) {
  if (cache && agora - cache.buscadaEm < VALIDADE_MS) return cache.cotacao;

  try {
    const cotacao = await consultarPtax(agora);
    if (cotacao) {
      cache = { cotacao, buscadaEm: agora };
      return cotacao;
    }
    console.warn('[gestao-geral] A PTAX não devolveu cotação no período.');
  } catch (err) {
    const motivo = err.name === 'TimeoutError' ? 'timeout' : err.message;
    console.warn(`[gestao-geral] Falha ao consultar a PTAX: ${motivo}`);
  }

  if (cache) return cache.cotacao;
  return { erro: true, status: 503, mensagem: 'Cotação do dólar indisponível no momento.' };
}
