import jwt from 'jsonwebtoken';

const authMiddleware = (req, res, next) => {
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
    req.userId = decoded.id;
    req.userRole = decoded.role;
    next();
  } catch (err) {
    return res.status(401).json({
      sucesso: false,
      mensagem: 'Token inválido ou expirado',
    });
  }
};

export const adminMiddleware = (req, res, next) => {
  if (req.userRole !== 'admin') {
    return res.status(403).json({
      sucesso: false,
      mensagem: 'Acesso negado — apenas administradores',
    });
  }
  next();
};

export default authMiddleware;
