/**
 * Competência é o mês de referência da cobrança, guardado como o dia 1.
 *
 * O horário nunca entra: `new Date('2026-08-01')` em UTC vira 31/07 num fuso
 * negativo, e a competência viraria o mês anterior. Por isso a manipulação toda
 * é feita em texto e em números, sem passar por Date com fuso.
 */

const FORMATO = /^(\d{4})-(\d{2})$/;

/** "2026-08" -> "2026-08-01", que é o que a coluna DATE espera. */
export function paraData(competencia) {
  const m = FORMATO.exec(String(competencia ?? '').trim());
  if (!m) return null;
  const mes = Number(m[2]);
  if (mes < 1 || mes > 12) return null;
  return `${m[1]}-${m[2]}-01`;
}

/** Date ou "2026-08-01" -> "2026-08". */
export function paraTexto(valor) {
  if (!valor) return null;
  if (valor instanceof Date) {
    // Componentes locais: o banco devolveu uma DATE, sem hora nem fuso.
    const mes = String(valor.getMonth() + 1).padStart(2, '0');
    return `${valor.getFullYear()}-${mes}`;
  }
  return String(valor).slice(0, 7);
}

/** Competência do mês corrente. */
export function atual(hoje = new Date()) {
  return `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, '0')}`;
}

/** Soma meses a uma competência, atravessando a virada do ano. */
export function somarMeses(competencia, meses) {
  const m = FORMATO.exec(competencia);
  if (!m) return null;
  const total = Number(m[1]) * 12 + (Number(m[2]) - 1) + meses;
  const ano = Math.floor(total / 12);
  const mes = (total % 12) + 1;
  return `${ano}-${String(mes).padStart(2, '0')}`;
}

/** Vencimento da competência, no dia configurado pelo condomínio. */
export function vencimentoDe(competencia, dia) {
  const m = FORMATO.exec(competencia);
  if (!m) return null;
  return `${m[1]}-${m[2]}-${String(dia).padStart(2, '0')}`;
}

export const ehValida = (competencia) => paraData(competencia) !== null;
