import * as model from '../models/prestacaoModel.js';
import { paraCentavos } from '../utils/dinheiro.js';
import { paraData, paraTexto } from '../utils/competencia.js';
import { resumir, urlSegura } from '../utils/prestacao.js';

const erro = (mensagem, status = 400) => ({ erro: true, mensagem, status });

const DATA = /^\d{4}-\d{2}-\d{2}$/;
const TIPOS = ['RECEITA', 'DESPESA'];

/** Relatório completo de uma competência: situação, lançamentos e totais. */
async function relatorio(condominioId, competencia) {
  const [situacao, lancamentos, receitaTaxas] = await Promise.all([
    model.situacao(condominioId, competencia),
    model.listarLancamentos(condominioId, competencia),
    model.receitaDeTaxas(condominioId, competencia),
  ]);
  return {
    ...situacao,
    competencia: paraTexto(competencia),
    receitaTaxasCentavos: receitaTaxas,
    lancamentos,
    totais: resumir(lancamentos, receitaTaxas),
  };
}

export async function obter(condominioId, competenciaTexto) {
  const competencia = paraData(competenciaTexto);
  if (!competencia) return erro('Competência inválida. Use AAAA-MM.');
  return relatorio(condominioId, competencia);
}

export async function lancar(condominioId, competenciaTexto, corpo, criadoPorId) {
  const competencia = paraData(competenciaTexto);
  if (!competencia) return erro('Competência inválida. Use AAAA-MM.');

  const tipo = String(corpo.tipo ?? '').toUpperCase();
  if (!TIPOS.includes(tipo)) return erro('Informe se é RECEITA ou DESPESA.');

  const categoria = String(corpo.categoria ?? '').trim();
  if (!categoria || categoria.length > 80) return erro('Informe a categoria (até 80 caracteres).');

  const descricao = String(corpo.descricao ?? '').trim();
  if (!descricao || descricao.length > 300) return erro('Informe a descrição (até 300 caracteres).');

  const valorCentavos = paraCentavos(corpo.valor ?? corpo.valorCentavos);
  if (!valorCentavos || valorCentavos <= 0) return erro('Informe um valor maior que zero.');

  const dataLancamento = corpo.dataLancamento;
  if (!DATA.test(dataLancamento ?? '')) return erro('Informe a data do lançamento (AAAA-MM-DD).');
  // A data precisa cair na competência: uma despesa de setembro lançada na
  // prestação de agosto distorce os dois meses.
  if (dataLancamento.slice(0, 7) !== competencia.slice(0, 7)) {
    return erro('A data do lançamento precisa estar dentro da competência.');
  }

  const comprovanteUrl = urlSegura(corpo.comprovanteUrl);
  if (comprovanteUrl === undefined) return erro('O comprovante precisa ser um link http ou https.');

  const lancamento = await model.lancar(condominioId, competencia, {
    tipo, categoria, descricao, valorCentavos, dataLancamento, comprovanteUrl, criadoPorId,
  });
  if (!lancamento) return erro('Esta competência já foi publicada e não aceita lançamentos.', 409);
  return lancamento;
}

export async function excluirLancamento(condominioId, id) {
  const existente = await model.lancamentoPorId(condominioId, id);
  if (!existente) return erro('Lançamento não encontrado.', 404);

  const excluido = await model.excluirLancamento(condominioId, id);
  if (!excluido) return erro('A competência deste lançamento já foi publicada.', 409);
  return { id };
}

/**
 * Publica a competência para os moradores. Depois disso, nada muda nela.
 *
 * Mês sem lançamento nem receita não é publicado: "prestação de contas" vazia
 * informaria ao morador que nada entrou nem saiu, o que não é verdade — só não
 * foi registrado.
 */
export async function publicar(condominioId, competenciaTexto, publicadoPorId) {
  const competencia = paraData(competenciaTexto);
  if (!competencia) return erro('Competência inválida. Use AAAA-MM.');

  const atual = await relatorio(condominioId, competencia);
  if (atual.status === 'PUBLICADA') return erro('Esta competência já foi publicada.', 409);
  if (!atual.lancamentos.length && atual.receitaTaxasCentavos === 0) {
    return erro('Não há lançamentos nem receita nesta competência para publicar.');
  }

  const publicada = await model.publicar(condominioId, competencia, publicadoPorId);
  if (!publicada) return erro('Esta competência já foi publicada.', 409);
  return relatorio(condominioId, competencia);
}

// ── Morador: só o que foi publicado ─────────────────────────────────────────

export async function listarPublicadas(condominioId) {
  const publicadas = await model.listarPublicadas(condominioId);
  return Promise.all(publicadas.map(async (p) => {
    const r = await relatorio(condominioId, p.competencia);
    return {
      competencia: r.competencia,
      publicadoEm: r.publicadoEm,
      receitasCentavos: r.totais.receitasCentavos,
      despesasCentavos: r.totais.despesasCentavos,
      saldoCentavos: r.totais.saldoCentavos,
    };
  }));
}

export async function obterPublicada(condominioId, competenciaTexto) {
  const competencia = paraData(competenciaTexto);
  if (!competencia) return erro('Competência inválida. Use AAAA-MM.');
  const r = await relatorio(condominioId, competencia);
  // Rascunho é 404 para o morador: o síndico ainda está fechando os números.
  if (r.status !== 'PUBLICADA') return erro('Prestação de contas não encontrada.', 404);
  // Quem lançou não é informação do morador.
  return { ...r, lancamentos: r.lancamentos.map(({ criadoPorId: _c, ...l }) => l), publicadoPorId: undefined };
}
