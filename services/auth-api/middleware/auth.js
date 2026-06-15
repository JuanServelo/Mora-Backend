import jwt from 'jsonwebtoken';
import User from '../models/User.js';
import {
  STATUS_USUARIO,
  podeAcessarAdmin,
  podeGerenciarUsuarios,
  isSuperAdmin,
} from '../constants/perfis.js';

const authMiddleware = async (req, res, next) => {
  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({
      sucesso: false,
      mensagem: 'Token não fornecido',
    });
  }

  const token = authHeader.split(' ')[1];

  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    const usuario = await User.findByPk(decoded.id);

    if (!usuario) {
      return res.status(401).json({
        sucesso: false,
        mensagem: 'Token inválido ou expirado',
      });
    }

    if (usuario.status === STATUS_USUARIO.INACTIVE) {
      return res.status(401).json({
        sucesso: false,
        mensagem: 'Esta conta foi desativada.',
      });
    }

    if (decoded.tokenVersion !== undefined
      && (usuario.tokenVersion || 0) !== decoded.tokenVersion) {
      return res.status(401).json({
        sucesso: false,
        mensagem: 'Token inválido ou expirado',
      });
    }

    req.userId = usuario.id;
    req.user = usuario;
    req.userPerfil = usuario.getPerfilEfetivo();
    next();
  } catch (err) {
    return res.status(401).json({
      sucesso: false,
      mensagem: 'Token inválido ou expirado',
    });
  }
};

export const adminMiddleware = (req, res, next) => {
  if (!podeAcessarAdmin(req.userPerfil)) {
    return res.status(403).json({
      sucesso: false,
      mensagem: 'Acesso negado — apenas administradores',
    });
  }
  next();
};

export const superAdminMiddleware = (req, res, next) => {
  if (!isSuperAdmin(req.userPerfil)) {
    return res.status(403).json({
      sucesso: false,
      mensagem: 'Acesso negado — apenas Super Admin.',
    });
  }
  next();
};

export const gestaoMiddleware = (req, res, next) => {
  if (podeGerenciarUsuarios(req.userPerfil)) {
    return next();
  }
  return res.status(403).json({
    sucesso: false,
    mensagem: 'Você não tem permissão para acessar esta funcionalidade.',
  });
};

export default authMiddleware;
