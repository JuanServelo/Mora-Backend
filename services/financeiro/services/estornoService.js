import { emTransacao } from '../config/database.js';
import * as asaas from '../gateway/asaasClient.js';
import * as estornosModel from '../models/estornosModel.js';
import * as faturasModel from '../models/faturasModel.js';
import * as cobrancasModel from '../models/cobrancasModel.js';
import * as plataformaModel from '../models/plataformaModel.js';
import * as notificacaoService from './notificacaoService.js';
import { desfechoDoEstorno } from '../utils/asaasStatus.js';
import { formatarBRL } from '../utils/dinheiro.js';
import { rotulo as rotuloCompetencia } from '../utils/competencia.js';

/**
 * Estorno: devolver, pelo gateway, um pagamento que ele recebeu.
 *
 * É assíncrono no Asaas. O pedido é aceito na hora, mas a devolução só se
 * conclui depois — às vezes só quando alguém autoriza a ação no painel da
 * conta. Por isso há dois momentos:
 *
 * 1. `pedir`: grava o pedido (SOLICITADO) e chama o gateway. A fatura continua
 *    PAGA, porque o dinheiro ainda não voltou.
 * 2. `concluirNa`: quando o gateway confirma — pelo webhook PAYMENT_REFUNDED ou
 *    pela conciliação, que pergunta a ele —, o pedido vira CONCLUIDO e a fatura,
 *    ESTORNADA.
 */

const erro = (mensagem, status = 400) => ({ erro: true, mensagem, status });

const MOTIVO_EXTERNO = 'Estornado direto no painel do Asaas';

/** Pede a devolução ao gateway e grava o pedido. Comum à fatura e à plataforma. */
export async function pedir(d) {
  let pedido;
  try {
    pedido = await estornosModel.criar(d);
  } catch (err) {
    if (err.code === '23505') return erro('Este pagamento já tem um estorno pedido ou concluído.', 409);
    throw err;
  }

  const r = await asaas.estornarPagamento(d.asaasPagamentoId, d.motivo);
  if (!r.ok) {
    // O pedido que falhou não bloqueia uma nova tentativa: o índice único
    // ignora os FALHOU.
    await estornosModel.marcarFalha(pedido.id, r.erro);
    return r.status === 400
      ? erro(`O gateway recusou o estorno: ${r.erro}`, 409)
      : erro('O gateway de pagamento não respondeu. Tente de novo em instantes.', 502);
  }

  if (desfechoDoEstorno(r.dados) === 'CONCLUIDO') await concluir(d.asaasPagamentoId);
  return estornosModel.porId(pedido.id);
}

/** O síndico estorna uma fatura paga pelo gateway. */
export async function estornarFatura(condominioId, faturaId, motivoTexto, usuarioId) {
  const motivo = String(motivoTexto ?? '').trim();
  if (motivo.length < 5) return erro('Explique o motivo do estorno (mínimo de 5 caracteres).');

  const fatura = await faturasModel.porId(condominioId, faturaId);
  if (!fatura) return erro('Fatura não encontrada.', 404);
  if (fatura.status !== 'PAGA') return erro('Só uma fatura paga pode ser estornada.', 409);
  if (fatura.formaBaixa !== 'GATEWAY') {
    return erro('Esta fatura foi baixada à mão: o dinheiro não passou pelo gateway, e a devolução é feita fora do sistema.', 409);
  }

  const cobranca = await cobrancasModel.recebidaDaFatura(fatura.id);
  if (!cobranca) return erro('Não há pagamento recebido pelo gateway para esta fatura.', 409);
  // O Asaas recusaria: só estorna PIX e cartão. Melhor dizer antes.
  if (cobranca.billingType !== 'PIX') {
    return erro('O gateway só estorna pagamentos feitos por PIX. Boleto pago se devolve por transferência, fora do sistema.', 409);
  }

  return pedir({
    condominioId,
    faturaId: fatura.id,
    asaasPagamentoId: cobranca.asaasId,
    valorCentavos: fatura.valorPagoCentavos ?? cobranca.valorCentavos,
    motivo,
    solicitadoPorId: usuarioId,
  });
}

/**
 * Conclui o estorno de um pagamento, dentro da transação de quem chama (o
 * webhook grava o evento na mesma transação). Repetível: um pagamento já
 * estornado não muda nada.
 *
 * Se o estorno foi feito direto no painel do Asaas, não há pedido nosso: ele é
 * criado aqui, já concluído, para o histórico não ter buraco.
 *
 * Devolve a fatura estornada (para notificar o morador) ou `null`.
 */
export async function concluirNa(cliente, asaasPagamentoId) {
  let pedido = await estornosModel.vigentePorPagamento(asaasPagamentoId, cliente);
  if (pedido?.status === 'CONCLUIDO') return null;

  if (!pedido) {
    const cobranca = await cobrancasModel.porAsaasId(asaasPagamentoId);
    const pagamento = cobranca ? null : await plataformaModel.pagamentoPorAsaasId(asaasPagamentoId, cliente);
    if (!cobranca && !pagamento) return null; // pagamento de fora do Mora
    const assinatura = pagamento && await plataformaModel.assinaturaPorId(pagamento.assinaturaId, cliente);
    pedido = await estornosModel.criar({
      condominioId: cobranca?.condominioId ?? assinatura.condominioId,
      faturaId: cobranca?.faturaId ?? null,
      pagamentoPlataformaId: pagamento?.id ?? null,
      asaasPagamentoId,
      valorCentavos: cobranca?.valorCentavos ?? pagamento.valorCentavos,
      motivo: MOTIVO_EXTERNO,
    }, cliente);
  }

  await estornosModel.marcarConcluido(pedido.id, cliente);

  if (pedido.pagamentoPlataformaId) {
    await plataformaModel.marcarPagamento(pedido.pagamentoPlataformaId, 'ESTORNADA', cliente);
    return null;
  }

  const cobranca = await cobrancasModel.porAsaasId(asaasPagamentoId);
  if (cobranca) await cobrancasModel.atualizar(cliente, cobranca.id, { status: 'ESTORNADA' });
  return faturasModel.marcarEstornada(cliente, pedido.faturaId, pedido.motivo);
}

/** Avisa o morador que o dinheiro voltou. */
export async function notificarEstorno(fatura) {
  if (!fatura) return;
  await notificacaoService.criar(
    fatura.responsavelUsuarioId, fatura.condominioId,
    'PAGAMENTO_ESTORNADO',
    'Pagamento estornado',
    `O pagamento da fatura de ${rotuloCompetencia(fatura.competencia)} foi devolvido: ${formatarBRL(fatura.valorPagoCentavos ?? fatura.valorCentavos)}. Motivo: ${fatura.estornoMotivo}`,
    { faturaId: fatura.id },
  );
}

export async function concluir(asaasPagamentoId) {
  const fatura = await emTransacao((cliente) => concluirNa(cliente, asaasPagamentoId));
  await notificarEstorno(fatura);
}

/**
 * Conciliação: pergunta ao Asaas como estão os pedidos em aberto.
 *
 * O webhook é o caminho normal, mas não alcança um ambiente local e pode se
 * perder. Roda antes de cada listagem e no job noturno, e é barata: só olha os
 * SOLICITADO.
 */
export async function conciliar(condominioId = null) {
  const abertos = await estornosModel.pendentes(condominioId);
  for (const pedido of abertos) {
    const r = await asaas.consultarCobranca(pedido.asaasPagamentoId);
    if (!r.ok) continue;
    const desfecho = desfechoDoEstorno(r.dados);
    if (desfecho === 'CONCLUIDO') await concluir(pedido.asaasPagamentoId);
    if (desfecho === 'FALHOU') await estornosModel.marcarFalha(pedido.id, 'Pedido cancelado no gateway.');
  }
}

/** Estornos das faturas do condomínio, já conciliados. */
export async function listarDoCondominio(condominioId) {
  await conciliar(condominioId);
  return estornosModel.listarDoCondominio(condominioId);
}
