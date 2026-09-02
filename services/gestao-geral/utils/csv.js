/**
 * Serializa linhas em CSV.
 *
 * Aspas duplicadas e o BOM no início são o que faz o Excel abrir acentuação
 * correta — sem o BOM, "Condomínio" vira "CondomÃ­nio".
 */
const escapar = (valor) => {
  const s = valor === null || valor === undefined ? '' : String(valor);
  return /[",;\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
};

export function paraCsv(linhas, colunas, separador = ';') {
  const cabecalho = colunas.join(separador);
  const corpo = linhas.map((l) => colunas.map((c) => escapar(l[c])).join(separador));
  return `﻿${[cabecalho, ...corpo].join('\r\n')}\r\n`;
}
