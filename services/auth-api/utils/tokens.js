import crypto from 'crypto';

/**
 * Gera um par (token cru, hash). O token cru vai para o usuário — por email ou
 * URL — e só o hash é persistido, então vazar o banco não permite usá-lo.
 */
export function gerarTokenComHash() {
  const token = crypto.randomBytes(32).toString('hex');
  return { token, hash: hashToken(token) };
}

export function hashToken(token) {
  return crypto.createHash('sha256').update(token).digest('hex');
}

export const expiraEm = (ms) => new Date(Date.now() + ms);
