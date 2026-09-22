import jwt from 'jsonwebtoken';

/**
 * Valida o JWT emitido pelo auth-api.
 *
 * Mesmo desenho do `gestao-geral` e do `financeiro`: token ausente ou inválido
 * é 401, sem exceção engolida. O contraexemplo está no `AuthFilter` do
 * portaria, que faz `catch (Exception ignored)` e deixa a requisição seguir —
 * é por isso que lá o `condominioId` é um parâmetro que o cliente escolhe.
 *
 * Aqui isso seria pior que de costume: conversa privada e caixa de entrada são
 * de uma pessoa só.
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

/** Quem responde pela administração do condomínio. */
export const PERFIS_GESTAO = ['ADMIN_GERAL', 'ADMIN_SINDICO'];

/** Quem mora na unidade e fala com a administração. */
export const PERFIS_UNIDADE = ['MORADOR', 'DONO_ALUGUEL'];

/** Perfis com caixa de entrada e conversa. O convidado não participa. */
export const PERFIS_COMUNICAVEIS = [...PERFIS_GESTAO, ...PERFIS_UNIDADE, 'PORTEIRO'];
