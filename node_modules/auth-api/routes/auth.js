import crypto from 'crypto';
import express from 'express';
import jwt from 'jsonwebtoken';
import passport from 'passport';
import rateLimit from 'express-rate-limit';
import { Op } from 'sequelize';
import User from '../models/User.js';
import authMiddleware from '../middleware/auth.js';
import { usuarioPublico } from '../utils/usuarioPublico.js';
import { enviarEmailReset } from '../utils/emailService.js';

const router = express.Router();

const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 10,
  message: { sucesso: false, mensagem: 'Muitas tentativas. Tente novamente em 15 minutos.' },
  standardHeaders: true,
  legacyHeaders: false,
});

const forgotLimiter = rateLimit({
  windowMs: 60 * 60 * 1000,
  max: 3,
  message: { sucesso: false, mensagem: 'Muitas solicitações de recuperação. Tente novamente em 1 hora.' },
  standardHeaders: true,
  legacyHeaders: false,
});

const getJwtSecret = () => process.env.JWT_SECRET;
const getFrontendUrl = () => process.env.FRONTEND_URL || 'http://localhost:5173';

const signToken = (userId, role = 'user') =>
  jwt.sign({ id: userId, role }, getJwtSecret(), { expiresIn: '7d' });

router.post('/register', authLimiter, async (req, res) => {
  try {
    const { nome, email, senha } = req.body;

    if (!nome || !email || !senha) {
      return res.status(400).json({ sucesso: false, mensagem: 'Nome, email e senha são obrigatórios' });
    }
    if (senha.length < 6) {
      return res.status(400).json({ sucesso: false, mensagem: 'Senha deve ter no mínimo 6 caracteres' });
    }

    const existeUsuario = await User.findOne({ where: { email: email.toLowerCase().trim() } });
    if (existeUsuario) {
      return res.status(400).json({ sucesso: false, mensagem: 'Email já cadastrado' });
    }

    const usuario = await User.create({ nome, email, senha, provider: 'local' });
    const token = signToken(usuario.id, usuario.role);

    res.status(201).json({
      sucesso: true,
      mensagem: 'Usuário criado com sucesso',
      token,
      usuario: usuarioPublico(usuario),
    });
  } catch (err) {
    console.error('Erro no register:', err);
    res.status(500).json({ sucesso: false, mensagem: err.message || 'Erro ao registrar' });
  }
});

router.post('/login', authLimiter, async (req, res) => {
  try {
    const { email, senha } = req.body;

    if (!email || !senha) {
      return res.status(400).json({ sucesso: false, mensagem: 'Email e senha são obrigatórios' });
    }

    const usuario = await User.scope('withPassword').findOne({ where: { email: email.toLowerCase().trim() } });
    if (!usuario || !usuario.senha) {
      return res.status(401).json({ sucesso: false, mensagem: 'Credenciais inválidas' });
    }

    const senhaValida = await usuario.compararSenha(senha);
    if (!senhaValida) {
      return res.status(401).json({ sucesso: false, mensagem: 'Credenciais inválidas' });
    }

    const token = signToken(usuario.id, usuario.role);

    res.json({
      sucesso: true,
      mensagem: 'Login realizado com sucesso',
      token,
      usuario: usuarioPublico(usuario),
    });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message || 'Erro ao fazer login' });
  }
});

router.get('/me', authMiddleware, async (req, res) => {
  try {
    const usuario = await User.findByPk(req.userId);
    if (!usuario) {
      return res.status(404).json({ sucesso: false, mensagem: 'Usuário não encontrado' });
    }
    res.json({
      sucesso: true,
      usuario: usuarioPublico(usuario),
    });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message || 'Erro ao buscar usuário' });
  }
});

router.put('/me', authMiddleware, async (req, res) => {
  try {
    const { nome, email, senha } = req.body;
    const usuario = await User.scope('withPassword').findByPk(req.userId);
    if (!usuario) {
      return res.status(404).json({ sucesso: false, mensagem: 'Usuário não encontrado' });
    }

    if (nome) usuario.nome = nome;
    if (email) usuario.email = email;
    if (senha) {
      if (senha.length < 6) {
        return res.status(400).json({ sucesso: false, mensagem: 'Senha deve ter no mínimo 6 caracteres' });
      }
      usuario.senha = senha;
    }

    await usuario.save();
    res.json({
      sucesso: true,
      mensagem: 'Perfil atualizado com sucesso',
      usuario: usuarioPublico(usuario),
    });
  } catch (err) {
    if (err.name === 'SequelizeUniqueConstraintError') {
      return res.status(400).json({ sucesso: false, mensagem: 'Email já cadastrado' });
    }
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao atualizar perfil' });
  }
});

router.post('/forgot-password', forgotLimiter, async (req, res) => {
  try {
    const { email } = req.body;
    if (!email) {
      return res.status(400).json({ sucesso: false, mensagem: 'Email é obrigatório' });
    }

    const usuario = await User.findOne({ where: { email: email.toLowerCase().trim() } });
    if (!usuario || usuario.provider !== 'local') {
      return res.json({ sucesso: true, mensagem: 'Se esse email estiver cadastrado, você receberá um link em breve.' });
    }

    const token = crypto.randomBytes(32).toString('hex');
    const hash = crypto.createHash('sha256').update(token).digest('hex');

    usuario.resetToken = hash;
    usuario.resetTokenExpira = new Date(Date.now() + 60 * 60 * 1000);
    await usuario.save({ validate: false });

    await enviarEmailReset(usuario.email, token);

    res.json({ sucesso: true, mensagem: 'Se esse email estiver cadastrado, você receberá um link em breve.' });
  } catch (err) {
    console.error('Erro no forgot-password:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao processar solicitação' });
  }
});

router.post('/reset-password/:token', authLimiter, async (req, res) => {
  try {
    const { senha } = req.body;
    if (!senha || senha.length < 6) {
      return res.status(400).json({ sucesso: false, mensagem: 'Senha deve ter no mínimo 6 caracteres' });
    }

    const hash = crypto.createHash('sha256').update(req.params.token).digest('hex');

    const usuario = await User.scope('withResetToken').findOne({
      where: {
        resetToken: hash,
        resetTokenExpira: { [Op.gt]: new Date() },
      },
    });

    if (!usuario) {
      return res.status(400).json({ sucesso: false, mensagem: 'Token inválido ou expirado' });
    }

    usuario.senha = senha;
    usuario.resetToken = null;
    usuario.resetTokenExpira = null;
    await usuario.save();

    res.json({ sucesso: true, mensagem: 'Senha redefinida com sucesso' });
  } catch (err) {
    console.error('Erro no reset-password:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao redefinir senha' });
  }
});

router.get('/google', (req, res, next) => {
  passport.authenticate('google', { scope: ['profile', 'email'] })(req, res, next);
});

router.get('/google/callback', (req, res) => {
  passport.authenticate('google', (err, user) => {
    if (err || !user) {
      return res.redirect(`${getFrontendUrl()}/?erro=oauth`);
    }
    const token = signToken(user.id, user.role);
    res.redirect(`${getFrontendUrl()}/auth/callback?token=${token}`);
  })(req, res);
});

export default router;
