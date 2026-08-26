import jwt from 'jsonwebtoken';

const PERFIL_ADMIN_GERAL = 'ADMIN_GERAL';

/**
 * Valida o JWT emitido pelo auth-api.
 *
 * Diferente do AuthFilter do portaria-service, que engole a exceção de parse e
 * deixa a requisição seguir: aqui token ausente ou inválido é 401, sem exceção.
 * Este endpoint expõe dados de TODOS os condomínios — é o lugar errado para
 * falhar aberto.
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

/** Números da plataforma inteira são exclusivos de quem opera a plataforma. */
export function exigirAdminGeral(req, res, next) {
  if (req.perfil !== PERFIL_ADMIN_GERAL) {
    return res.status(403).json({
      sucesso: false,
      mensagem: 'Apenas o Admin Geral pode consultar dados da plataforma.',
    });
  }
  next();
}
