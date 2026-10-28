import jwt from 'jsonwebtoken';
import { SERVICOS } from '../config/servicos.js';
import { buscar } from '../clients/httpClient.js';

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

/**
 * Confirma no auth-api que a sessão ainda vale, e fixa quem é o usuário.
 *
 * A assinatura do JWT (`autenticar`) prova que o token foi emitido pelo
 * auth-api, mas não que continua válido: um usuário desativado, ou com a
 * sessão encerrada (o `tokenVersion` mudou), segue com um token de assinatura
 * perfeita até ele expirar. Para ler dados de outros serviços isso não pesa —
 * eles revalidam o token repassado. Para gravar no banco deste serviço, pesa:
 * quem revalida é este middleware.
 *
 * O id vem da resposta do auth-api, e nunca da URL ou do corpo: cada um só
 * alcança o que é seu. Sem resposta do auth-api, recusa (503) em vez de deixar
 * passar — falhar fechado.
 */
export function exigirSessaoValida(req, res, next) {
  buscar(`${SERVICOS.auth}/api/auth/me`, req.authorization)
    .then((r) => {
      if (r.ok && r.dados?.usuario?.id != null) {
        req.usuarioId = String(r.dados.usuario.id);
        return next();
      }
      if (r.status === 401 || r.status === 403) {
        return res.status(401).json({ sucesso: false, mensagem: 'Sessão inválida ou encerrada. Entre de novo.' });
      }
      return res.status(503).json({ sucesso: false, mensagem: 'Não foi possível confirmar a sessão agora. Tente novamente.' });
    })
    .catch(next);
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
