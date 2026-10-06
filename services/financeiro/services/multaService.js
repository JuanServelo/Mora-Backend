import * as model from '../models/multasModel.js';
import * as regrasTaxaModel from '../models/regrasTaxaModel.js';
import * as notificacaoService from './notificacaoService.js';
import { listarUnidades } from '../clients/portariaClient.js';
import { listarResidentesPorUnidade } from '../clients/authClient.js';
import { paraCentavos, formatarBRL } from '../utils/dinheiro.js';
import { podeRecorrer, prazoRecurso, transicao, faturavel } from '../utils/multas.js';

const erro = (mensagem, status = 400) => ({ erro: true, mensagem, status });

const DATA = /^\d{4}-\d{2}-\d{2}$/;

/** Acrescenta o que a tela precisa e o banco não guarda: prazo e se dá para recorrer. */
function comPrazo(multa, diasRecurso) {
  return {
    ...multa,
    prazoRecurso: prazoRecurso(multa.criadoEm, diasRecurso),
    podeRecorrer: podeRecorrer(multa, diasRecurso),
    aguardandoCobranca: faturavel(multa, diasRecurso),
  };
}

async function diasDeRecurso(condominioId) {
  const regras = await regrasTaxaModel.obterOuCriar(condominioId);
  return regras.diasRecursoMulta;
}

/**
 * A quem avisar da multa: a pessoa multada, ou o responsável financeiro da
 * unidade quando a infração é da unidade. Falha macia — sem destinatário, a
 * multa continua aplicada, só não gera aviso.
 */
async function destinatario(multa, authorization) {
  if (multa.usuarioId) return multa.usuarioId;
  const r = await listarResidentesPorUnidade(multa.unidadeId, authorization);
  if (!r.ok) return null;
  return r.dados?.moradores?.find((m) => m.responsavelFinanceiro)?.id ?? null;
}

export async function listar(condominioId, filtros = {}) {
  const [multas, dias] = await Promise.all([
    model.listar(condominioId, filtros),
    diasDeRecurso(condominioId),
  ]);
  return multas.map((m) => comPrazo(m, dias));
}

export async function listarDaUnidade(condominioId, unidadeId) {
  return listar(condominioId, { unidadeId });
}

export async function aplicar(condominioId, corpo, aplicadaPorId, authorization) {
  const unidadeId = corpo.unidadeId;
  if (!unidadeId) return erro('Informe a unidade.');

  const valorCentavos = paraCentavos(corpo.valor ?? corpo.valorCentavos);
  if (!valorCentavos || valorCentavos <= 0) return erro('Informe um valor maior que zero.');

  const motivo = String(corpo.motivo ?? '').trim();
  if (motivo.length < 5) return erro('Descreva o motivo da multa.');

  const dataInfracao = corpo.dataInfracao;
  if (!DATA.test(dataInfracao ?? '')) return erro('Informe a data da infração (AAAA-MM-DD).');
  // Comparação em texto: as duas datas estão no mesmo formato e sem hora.
  const hoje = new Date().toLocaleDateString('sv-SE', { timeZone: 'America/Sao_Paulo' });
  if (dataInfracao > hoje) return erro('A data da infração não pode estar no futuro.');

  // A unidade precisa ser deste condomínio. O id vem do corpo; sem conferir, o
  // síndico multaria uma unidade de outro cliente sabendo o id dela.
  const unidades = await listarUnidades(condominioId, authorization);
  if (!unidades.ok) return erro('Não foi possível confirmar a unidade. Tente novamente.', 503);
  if (!unidades.dados.some((u) => u.id === unidadeId)) {
    return erro('Unidade não encontrada neste condomínio.', 404);
  }

  let usuarioId = null;
  if (corpo.usuarioId) {
    usuarioId = Number(corpo.usuarioId);
    const residentes = await listarResidentesPorUnidade(unidadeId, authorization);
    if (!residentes.ok) return erro('Não foi possível confirmar o morador. Tente novamente.', 503);
    if (!residentes.dados?.moradores?.some((m) => m.id === usuarioId)) {
      return erro('Este morador não pertence à unidade informada.');
    }
  }

  const multa = await model.criar(condominioId, {
    unidadeId, usuarioId, valorCentavos, motivo, dataInfracao, aplicadaPorId,
  });

  const dias = await diasDeRecurso(condominioId);
  const resposta = comPrazo(multa, dias);

  const para = await destinatario(multa, authorization);
  await notificacaoService.criar(
    para, condominioId, 'MULTA_APLICADA',
    'Multa aplicada à sua unidade',
    `${motivo} — ${formatarBRL(valorCentavos)}. Você pode recorrer até ${resposta.prazoRecurso}.`,
    { multaId: multa.id },
  );

  return resposta;
}

/** Recurso do morador, só para multa da própria unidade e dentro do prazo. */
export async function recorrer(condominioId, unidadeId, id, corpo) {
  const texto = String(corpo.texto ?? corpo.recurso ?? '').trim();
  if (texto.length < 10) return erro('Explique o recurso em pelo menos 10 caracteres.');

  const multa = await model.porId(condominioId, id);
  // De outra unidade é 404, e não 403: dizer "existe, mas não é sua" entregaria
  // a multa do vizinho.
  if (!multa || multa.unidadeId !== unidadeId) return erro('Multa não encontrada.', 404);

  const dias = await diasDeRecurso(condominioId);
  if (multa.recursoTexto) return erro('Esta multa já teve recurso.', 409);
  if (!transicao('recorrer', multa.status)) {
    return erro('Esta multa não está aberta a recurso.', 409);
  }
  if (!podeRecorrer(multa, dias)) {
    return erro(`O prazo de recurso terminou em ${prazoRecurso(multa.criadoEm, dias)}.`, 409);
  }

  const atualizada = await model.registrarRecurso(condominioId, id, texto);
  if (!atualizada) return erro('A multa mudou enquanto o recurso era enviado. Atualize a tela.', 409);
  return comPrazo(atualizada, dias);
}

/** Julgamento do recurso pelo síndico. Justificativa obrigatória nos dois casos. */
export async function julgar(condominioId, id, corpo, julgadoPorId, authorization) {
  const aceito = corpo.aceito === true || corpo.decisao === 'ACEITO';
  const recusado = corpo.aceito === false || corpo.decisao === 'RECUSADO';
  if (!aceito && !recusado) return erro('Informe a decisão: aceito ou recusado.');

  const justificativa = String(corpo.justificativa ?? '').trim();
  if (justificativa.length < 5) return erro('Explique a decisão.');

  const multa = await model.porId(condominioId, id);
  if (!multa) return erro('Multa não encontrada.', 404);

  const novoStatus = transicao(aceito ? 'aceitarRecurso' : 'recusarRecurso', multa.status);
  if (!novoStatus) return erro('Só é possível julgar multa em recurso.', 409);

  const atualizada = await model.julgar(condominioId, id, novoStatus, julgadoPorId, justificativa);
  if (!atualizada) return erro('A multa mudou enquanto era julgada. Atualize a tela.', 409);

  const para = await destinatario(atualizada, authorization);
  await notificacaoService.criar(
    para, condominioId, 'MULTA_JULGADA',
    aceito ? 'Recurso aceito: multa cancelada' : 'Recurso recusado: multa mantida',
    justificativa,
    { multaId: id },
  );

  return comPrazo(atualizada, await diasDeRecurso(condominioId));
}

/**
 * Cancelamento pelo síndico, com ou sem recurso.
 *
 * Se a multa já entrou numa fatura, ela não é tirada de lá: o próximo
 * fechamento da unidade leva um estorno do mesmo valor.
 */
export async function cancelar(condominioId, id, corpo, julgadoPorId) {
  const justificativa = String(corpo.justificativa ?? '').trim();
  if (justificativa.length < 5) return erro('Explique o motivo do cancelamento.');

  const multa = await model.porId(condominioId, id);
  if (!multa) return erro('Multa não encontrada.', 404);
  if (!transicao('cancelar', multa.status)) return erro('Esta multa já está cancelada.', 409);

  const atualizada = await model.cancelar(condominioId, id, julgadoPorId, justificativa);
  if (!atualizada) return erro('A multa mudou enquanto era cancelada. Atualize a tela.', 409);

  return {
    ...comPrazo(atualizada, await diasDeRecurso(condominioId)),
    estornoNoProximoFechamento: Boolean(atualizada.faturaItemId),
  };
}
