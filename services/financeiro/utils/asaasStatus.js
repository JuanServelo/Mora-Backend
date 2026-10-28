/**
 * Tradução dos estados do Asaas para os nossos. Funções puras: são elas que os
 * testes exercitam, sem rede.
 */

const RECEBIDO = ['RECEIVED', 'CONFIRMED', 'RECEIVED_IN_CASH'];
// Pedido de estorno aberto não devolveu nada ainda: o dinheiro continua aqui.
const ESTORNO_EM_CURSO = ['REFUND_REQUESTED', 'REFUND_IN_PROGRESS'];

/** Status de uma cobrança do Asaas → status de `pagamentos_plataforma`. */
export function statusDoPagamento(statusAsaas) {
  if (RECEBIDO.includes(statusAsaas) || ESTORNO_EM_CURSO.includes(statusAsaas)) return 'RECEBIDA';
  if (statusAsaas === 'OVERDUE') return 'VENCIDA';
  if (statusAsaas === 'REFUNDED') return 'ESTORNADA';
  return 'PENDENTE';
}

/** Cobrança do Asaas (API ou webhook) → linha de `pagamentos_plataforma`. */
export function pagamentoDoAsaas(p) {
  return {
    asaasPagamentoId: p.id,
    valorCentavos: Math.round(Number(p.value ?? 0) * 100),
    vencimento: p.dueDate,
    status: statusDoPagamento(p.status),
    forma: p.billingType ?? null,
    // A fatura do Asaas mostra boleto e PIX na mesma página.
    urlPagamento: p.invoiceUrl ?? p.bankSlipUrl ?? null,
    pagoEm: p.paymentDate ?? p.clientPaymentDate ?? null,
  };
}

/**
 * Em que pé está a devolução de um pagamento, olhando a cobrança do Asaas.
 *
 * - `CONCLUIDO`: o pagamento está REFUNDED, ou o último pedido de estorno
 *   terminou (DONE);
 * - `FALHOU`: o último pedido foi cancelado — na autorização da conta, por
 *   exemplo;
 * - `PENDENTE`: qualquer outra coisa, inclusive aguardando autorização.
 */
export function desfechoDoEstorno(pagamento) {
  if (pagamento?.status === 'REFUNDED') return 'CONCLUIDO';
  const ultimo = (pagamento?.refunds ?? []).at(-1);
  if (ultimo?.status === 'DONE') return 'CONCLUIDO';
  if (ultimo?.status === 'CANCELLED') return 'FALHOU';
  return 'PENDENTE';
}
