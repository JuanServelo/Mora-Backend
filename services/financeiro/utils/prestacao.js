/**
 * Totais da prestação de contas de uma competência.
 *
 * A receita das taxas não é lançada à mão: vem das faturas pagas no mês e entra
 * aqui como uma linha a mais. Lançá-la manualmente abriria espaço para o número
 * divulgado aos moradores não bater com o que de fato entrou.
 */
export function resumir(lancamentos, receitaTaxasCentavos = 0) {
  let receitas = receitaTaxasCentavos;
  let despesas = 0;
  const porCategoria = new Map();

  for (const l of lancamentos) {
    if (l.tipo === 'RECEITA') receitas += l.valorCentavos;
    else despesas += l.valorCentavos;

    const chave = `${l.tipo}:${l.categoria}`;
    const atual = porCategoria.get(chave) ?? { tipo: l.tipo, categoria: l.categoria, valorCentavos: 0 };
    atual.valorCentavos += l.valorCentavos;
    porCategoria.set(chave, atual);
  }

  if (receitaTaxasCentavos > 0) {
    porCategoria.set('RECEITA:Taxas condominiais', {
      tipo: 'RECEITA', categoria: 'Taxas condominiais', valorCentavos: receitaTaxasCentavos, automatica: true,
    });
  }

  return {
    receitasCentavos: receitas,
    despesasCentavos: despesas,
    saldoCentavos: receitas - despesas,
    // Maior primeiro: é o que o morador quer ver — para onde foi o dinheiro.
    porCategoria: [...porCategoria.values()].sort((a, b) => b.valorCentavos - a.valorCentavos),
  };
}

/** Só aceita link http(s): um `javascript:` no comprovante viraria script na tela. */
export function urlSegura(valor) {
  if (!valor) return null;
  try {
    const u = new URL(String(valor).trim());
    return u.protocol === 'http:' || u.protocol === 'https:' ? u.toString() : undefined;
  } catch {
    return undefined;
  }
}
