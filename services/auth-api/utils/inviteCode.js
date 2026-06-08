import crypto from 'crypto';

const CHARSET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

export function gerarCodigoConvite(tamanho = 8) {
  const bytes = crypto.randomBytes(tamanho);
  let codigo = '';
  for (let i = 0; i < tamanho; i += 1) {
    codigo += CHARSET[bytes[i] % CHARSET.length];
  }
  return codigo;
}

export function normalizarCodigo(codigo) {
  return String(codigo || '').trim().toUpperCase();
}
