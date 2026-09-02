/**
 * Valores monetários circulam como BIGINT em centavos.
 *
 * A conversão acontece só nas bordas: entrada do usuário aqui, saída para a
 * tela ali. No meio, tudo é inteiro — soma, rateio e comparação são exatos, sem
 * o `0.1 + 0.2` que estraga fechamento de mês.
 */

/** Aceita "249,90", "249.90", 249.9 e devolve 24990. Recusa o resto. */
export function paraCentavos(entrada) {
  if (entrada === null || entrada === undefined || entrada === '') return null;

  if (typeof entrada === 'number') {
    if (!Number.isFinite(entrada)) return null;
    return Math.round(entrada * 100);
  }

  const texto = String(entrada).trim().replace(/\s/g, '');
  // Vírgula como decimal é o formato que o usuário brasileiro digita.
  const normalizado = texto.includes(',') ? texto.replace(/\./g, '').replace(',', '.') : texto;

  if (!/^-?\d+(\.\d{1,2})?$/.test(normalizado)) return null;
  return Math.round(Number(normalizado) * 100);
}

/** 24990 -> "R$ 249,90". Só para exibição; nunca reentre este texto no cálculo. */
export function formatarBRL(centavos) {
  if (centavos === null || centavos === undefined) return null;
  return (centavos / 100).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

/** 24990 -> "249.90", que é o que a API do gateway espera. */
export function paraDecimalString(centavos) {
  const sinal = centavos < 0 ? '-' : '';
  const abs = Math.abs(centavos);
  return `${sinal}${Math.floor(abs / 100)}.${String(abs % 100).padStart(2, '0')}`;
}

/**
 * Divide `total` entre os pesos dados, sem perder nem inventar centavo.
 *
 * A divisão proporcional quase nunca dá inteiro. Arredondar cada parcela e somar
 * dá um total diferente do configurado — poucos centavos por mês, mas o
 * condomínio arrecada errado e ninguém percebe. Aqui as parcelas são truncadas,
 * e a sobra vai integralmente para a de maior peso.
 *
 * @param {number} total centavos a distribuir
 * @param {Array<{ chave: any, peso: number }>} pesos
 * @returns {Map<any, number>} centavos por chave, somando exatamente `total`
 */
export function ratear(total, pesos) {
  const soma = pesos.reduce((acc, p) => acc + p.peso, 0);
  if (soma <= 0) throw new Error('Soma dos pesos precisa ser positiva para ratear.');

  const parcelas = new Map();
  let distribuido = 0;

  for (const { chave, peso } of pesos) {
    const parcela = Math.floor((total * peso) / soma);
    parcelas.set(chave, parcela);
    distribuido += parcela;
  }

  const sobra = total - distribuido;
  if (sobra !== 0) {
    const maior = pesos.reduce((a, b) => (b.peso > a.peso ? b : a));
    parcelas.set(maior.chave, parcelas.get(maior.chave) + sobra);
  }

  return parcelas;
}
