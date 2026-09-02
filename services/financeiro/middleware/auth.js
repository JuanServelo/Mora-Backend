import jwt from 'jsonwebtoken';

/**
 * Valida o JWT emitido pelo auth-api.
 *
 * Mesmo desenho do `gestao-geral`: token ausente ou inválido é 401, sem exceção
 * engolida. Aqui pesa ainda mais que num painel — estas rotas movem dinheiro.
 */
export function autenticar(req, res, next) {
  const header = req.headers.authorization;

  if (!header || !header.startsWith('Bearer ')) {
    return res.status(401).json({ sucesso: false, mensagem: 'Token não fornecido' });
  }

  const token = header.slice('Bearer '.length).trim();

  try {
    const claims = jwt.verify(token, process.env.JWT_SECRET);
    req.claims = claims;
    req.perfil = claims.perfil;
    // Repassado adiante: o auth-api revalida, inclusive a revogação por tokenVersion.
    req.authorization = header;
    return next();
  } catch {
    return res.status(401).json({ sucesso: false, mensagem: 'Token inválido ou expirado' });
  }
}

/** Restringe a rota aos perfis listados. Use depois de `autenticar`. */
export function exigirPerfis(...perfis) {
  return (req, res, next) => {
    if (!perfis.includes(req.perfil)) {
      return res.status(403).json({
        sucesso: false,
        mensagem: 'Seu perfil não tem acesso a esta operação.',
      });
    }
    next();
  };
}

/** Quem administra o financeiro de um condomínio. */
export const PERFIS_GESTAO = ['ADMIN_GERAL', 'ADMIN_SINDICO'];

/** Quem enxerga as próprias faturas. */
export const PERFIS_UNIDADE = ['MORADOR', 'DONO_ALUGUEL'];
