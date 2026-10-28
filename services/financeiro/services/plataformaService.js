import * as asaas from '../gateway/asaasClient.js';
import * as plataformaModel from '../models/plataformaModel.js';
import * as estornoService from './estornoService.js';
import { condominio as buscarCondominio } from '../clients/authClient.js';
import { assinaturaVigente } from '../clients/planClient.js';
import { pagamentoDoAsaas } from '../utils/asaasStatus.js';

/**
 * Mensalidade do Mora em cobrança recorrente.
 *
 * O outro fluxo de dinheiro da plataforma: o morador paga o condomínio (as
 * faturas); o condomínio paga o Mora. Este é pago por uma assinatura no
 * Asaas, que gera a cobrança de cada mês sozinho — não há job nosso nem
 * fechamento. O Admin Geral ativa; o síndico vê e paga.
 *
 * O plano e o valor vêm do plan-service na ativação e ficam gravados: são o
 * que foi contratado no gateway.
 */

const erro = (mensagem, status = 400) => ({ erro: true, mensagem, status });
const FORMAS = ['PIX', 'BOLETO'];
const hojeLocal = () => new Date().toLocaleDateString('sv-SE', { timeZone: 'America/Sao_Paulo' });

/**
 * Traz para cá as cobranças que o Asaas gerou para a assinatura.
 *
 * O webhook faz o mesmo a cada evento; isto cobre o que ele perdeu ou não
 * alcançou (ambiente local). Se o gateway não responder, fica o que já está
 * gravado.
 */
export async function conciliarPagamentos(assinatura) {
  const r = await asaas.listarPagamentosDaAssinatura(assinatura.asaasAssinaturaId);
  if (r.ok) {
    for (const p of r.dados?.data ?? []) {
      await plataformaModel.registrarPagamento(assinatura.id, pagamentoDoAsaas(p));
    }
  }
  return plataformaModel.pagamentosDaAssinatura(assinatura.id);
}

const comPagamentos = async (a) => ({
  ...a,
  pagamentos: a.status === 'ATIVA'
    ? await conciliarPagamentos(a)
    : await plataformaModel.pagamentosDaAssinatura(a.id),
});

export async function ativar(corpo, usuarioId, authorization) {
  const condominioId = String(corpo?.condominioId ?? '').trim();
  const forma = String(corpo?.forma ?? 'BOLETO').toUpperCase();
  const proximoVencimento = corpo?.proximoVencimento ?? hojeLocal();

  if (!condominioId) return erro('Informe o condomínio em condominioId.');
  if (!FORMAS.includes(forma)) return erro('Forma inválida. Use PIX ou BOLETO.');
  if (!/^\d{4}-\d{2}-\d{2}$/.test(proximoVencimento) || proximoVencimento < hojeLocal()) {
    return erro('O primeiro vencimento precisa ser hoje ou uma data futura (AAAA-MM-DD).');
  }
  if (await plataformaModel.ativaDoCondominio(condominioId)) {
    return erro('Este condomínio já tem cobrança recorrente ativa. Cancele a atual para trocar.', 409);
  }

  const [cond, plano] = await Promise.all([
    buscarCondominio(condominioId, authorization),
    assinaturaVigente(condominioId, authorization),
  ]);

  if (!cond.ok) {
    return cond.status === 404 ? erro('Condomínio não encontrado.', 404) : erro('Não foi possível consultar o condomínio. Tente novamente.', 503);
  }
  if (!plano.ok) {
    return plano.status === 404
      ? erro('O condomínio não tem plano ativo no plan-service. Vincule um plano antes de cobrar.', 409)
      : erro('Não foi possível consultar o plano do condomínio. Tente novamente.', 503);
  }

  const documento = String(cond.dados?.cnpj ?? '').replace(/\D/g, '');
  if (!documento) return erro('Cadastre o CNPJ do condomínio: o gateway exige o documento do pagador.');

  const valorCentavos = Math.round(Number(plano.dados.mensalidade) * 100);
  if (!(valorCentavos > 0)) return erro('O plano do condomínio não tem mensalidade para cobrar.', 409);

  const cliente = await asaas.garantirCliente({
    nome: cond.dados.nome,
    cpf: documento,
    email: cond.dados.email ?? undefined,
    usuarioId: condominioId,
  });
  if (!cliente.ok) return erro(`O gateway recusou o cadastro do condomínio: ${cliente.erro}`, 502);

  const assinaturaAsaas = await asaas.criarAssinatura({
    clienteId: cliente.dados.id,
    forma,
    valorCentavos,
    proximoVencimento,
    descricao: `Mensalidade Mora - plano ${plano.dados.planNome}`,
    condominioId,
  });
  if (!assinaturaAsaas.ok) return erro(`O gateway recusou a assinatura: ${assinaturaAsaas.erro}`, 502);

  let assinatura;
  try {
    assinatura = await plataformaModel.criarAssinatura({
      condominioId,
      planoNome: plano.dados.planNome,
      planoAssinaturaId: plano.dados.id,
      asaasAssinaturaId: assinaturaAsaas.dados.id,
      asaasClienteId: cliente.dados.id,
      forma,
      valorCentavos,
      ativadaPorId: usuarioId,
    });
  } catch (err) {
    // Duas ativações ao mesmo tempo: o índice parcial recusa a segunda. A
    // assinatura que ela criou no gateway não pode ficar cobrando sozinha.
    if (err.code !== '23505') throw err;
    await asaas.cancelarAssinatura(assinaturaAsaas.dados.id);
    return erro('Este condomínio já tem cobrança recorrente ativa.', 409);
  }

  return comPagamentos(assinatura);
}

/** Todas as assinaturas, para o Admin Geral, com as cobranças conciliadas. */
export async function listar() {
  const assinaturas = await plataformaModel.listarAssinaturas();
  return Promise.all(assinaturas.map(comPagamentos));
}

/** A do condomínio do síndico, para ele ver e pagar. `null` se não houver. */
export async function doCondominio(condominioId) {
  const a = await plataformaModel.maisRelevanteDoCondominio(condominioId);
  return a ? comPagamentos(a) : null;
}

export async function cancelar(id) {
  const a = await plataformaModel.assinaturaPorId(id);
  if (!a) return erro('Assinatura não encontrada.', 404);
  if (a.status !== 'ATIVA') return erro('Esta assinatura já está cancelada.', 409);

  const r = await asaas.cancelarAssinatura(a.asaasAssinaturaId);
  // 404: já não existe lá, e o que importa é parar de cobrar.
  if (!r.ok && r.status !== 404) return erro('O gateway não confirmou o cancelamento. Tente novamente.', 502);

  const cancelada = await plataformaModel.cancelarAssinatura(id);
  return { ...cancelada, pagamentos: await plataformaModel.pagamentosDaAssinatura(id) };
}

/** Devolve uma mensalidade recebida. Mesma regra do estorno de fatura: só PIX. */
export async function estornarPagamento(pagamentoId, motivoTexto, usuarioId) {
  const motivo = String(motivoTexto ?? '').trim();
  if (motivo.length < 5) return erro('Explique o motivo do estorno (mínimo de 5 caracteres).');

  const p = await plataformaModel.pagamentoPorId(pagamentoId);
  if (!p) return erro('Pagamento não encontrado.', 404);
  if (p.status !== 'RECEBIDA') return erro('Só um pagamento recebido pode ser estornado.', 409);
  if (p.forma !== 'PIX') {
    return erro('O gateway só estorna pagamentos feitos por PIX. Boleto pago se devolve por transferência, fora do sistema.', 409);
  }

  const a = await plataformaModel.assinaturaPorId(p.assinaturaId);
  return estornoService.pedir({
    condominioId: a.condominioId,
    pagamentoPlataformaId: p.id,
    asaasPagamentoId: p.asaasPagamentoId,
    valorCentavos: p.valorCentavos,
    motivo,
    solicitadoPorId: usuarioId,
  });
}

/**
 * Webhook de uma cobrança gerada por assinatura. Devolve `false` se a
 * assinatura não é nossa, para o webhook seguir com os outros tratamentos.
 */
export async function registrarDoWebhook(cliente, evento, pagamento) {
  const a = await plataformaModel.assinaturaPorAsaasId(pagamento.subscription, cliente);
  if (!a) return false;
  const linha = pagamentoDoAsaas(pagamento);
  if (evento === 'PAYMENT_DELETED') linha.status = 'CANCELADA';
  await plataformaModel.registrarPagamento(a.id, linha, cliente);
  return true;
}
