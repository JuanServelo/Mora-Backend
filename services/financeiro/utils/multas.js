/**
 * Regras da multa que não dependem de banco.
 *
 * Ficam aqui, separadas do service, para serem testadas sem Postgres: são elas
 * que decidem se um morador ainda pode recorrer e se a multa já pode ser
 * cobrada — os dois pontos em que um erro custa dinheiro a alguém.
 *
 * Datas são comparadas como texto `YYYY-MM-DD` no fuso do condomínio. Comparar
 * `Date` com hora faria uma multa aplicada às 23h contar o prazo a partir do dia
 * seguinte em UTC.
 */

const FUSO = 'America/Sao_Paulo';

/** Date ou ISO -> "YYYY-MM-DD" no fuso do condomínio. */
export function dataLocal(valor) {
  if (!valor) return null;
  const d = valor instanceof Date ? valor : new Date(valor);
  if (Number.isNaN(d.getTime())) return null;
  // `sv-SE` formata como YYYY-MM-DD.
  return d.toLocaleDateString('sv-SE', { timeZone: FUSO });
}

/** Soma dias a uma data "YYYY-MM-DD", sem fuso no caminho. */
export function somarDias(data, dias) {
  const [a, m, d] = data.split('-').map(Number);
  const t = new Date(Date.UTC(a, m - 1, d + dias));
  return t.toISOString().slice(0, 10);
}

/**
 * Último dia em que o morador ainda pode recorrer.
 *
 * Conta da aplicação, não da infração: o morador só fica sabendo quando a
 * multa é registrada, e um prazo correndo antes disso poderia nascer vencido.
 */
export function prazoRecurso(aplicadaEm, diasRecurso) {
  const inicio = dataLocal(aplicadaEm);
  if (!inicio) return null;
  return somarDias(inicio, Math.max(0, Number(diasRecurso) || 0));
}

/** Se `hoje` ainda está dentro do prazo (o último dia conta). */
export function dentroDoPrazo(aplicadaEm, diasRecurso, hoje = new Date()) {
  const prazo = prazoRecurso(aplicadaEm, diasRecurso);
  return prazo !== null && dataLocal(hoje) <= prazo;
}

/** Se o morador pode recorrer agora. */
export function podeRecorrer(multa, diasRecurso, hoje = new Date()) {
  return multa.status === 'APLICADA'
    // Um recurso por multa: recusado, a decisão vale.
    && !multa.recursoTexto
    && !multa.faturaItemId
    && dentroDoPrazo(multa.criadoEm, diasRecurso, hoje);
}

/**
 * Se a multa entra no próximo fechamento.
 *
 * Só depois de o prazo de recurso vencer: cobrar antes tiraria do morador o
 * direito de contestar, e uma multa em recurso nunca é cobrada.
 */
export function faturavel(multa, diasRecurso, hoje = new Date()) {
  return multa.status === 'APLICADA'
    && !multa.faturaItemId
    && !dentroDoPrazo(multa.criadoEm, diasRecurso, hoje);
}

/** Multa cancelada depois de faturada, cujo estorno ainda não foi lançado. */
export function estornoPendente(multa) {
  return multa.status === 'CANCELADA'
    && Boolean(multa.faturaItemId)
    && !multa.estornoFaturaItemId;
}

/**
 * Transições permitidas. Qualquer outra é recusada com 409.
 *
 * CANCELADA é final: reabrir uma multa cancelada depois de estornada criaria
 * uma cobrança que ninguém aplicou de novo.
 */
const TRANSICOES = {
  recorrer: { de: ['APLICADA'], para: 'EM_RECURSO' },
  aceitarRecurso: { de: ['EM_RECURSO'], para: 'CANCELADA' },
  recusarRecurso: { de: ['EM_RECURSO'], para: 'APLICADA' },
  cancelar: { de: ['APLICADA', 'EM_RECURSO'], para: 'CANCELADA' },
};

export function transicao(acao, statusAtual) {
  const t = TRANSICOES[acao];
  if (!t) return null;
  return t.de.includes(statusAtual) ? t.para : null;
}
