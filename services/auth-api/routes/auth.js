import express from 'express';
import jwt from 'jsonwebtoken';
import mongoose from 'mongoose';
import passport from 'passport';
import rateLimit from 'express-rate-limit';
import User from '../models/User.js';
import authMiddleware from '../middleware/auth.js';

const router = express.Router();

// Rate limiter para rotas de autenticação (10 tentativas por 15 min)
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 10,
  message: { sucesso: false, mensagem: 'Muitas tentativas. Tente novamente em 15 minutos.' },
  standardHeaders: true,
  legacyHeaders: false,
});

// Lidos em runtime para garantir que dotenv já carregou
const getJwtSecret = () => process.env.JWT_SECRET;
const getFrontendUrl = () => process.env.FRONTEND_URL || 'http://localhost:5173';

const signToken = (userId, role = 'user') =>
  jwt.sign({ id: userId, role }, getJwtSecret(), { expiresIn: '7d' });

// POST /api/auth/register
router.post('/register', authLimiter, async (req, res) => {
  try {
    const { nome, email, senha } = req.body;

    if (!nome || !email || !senha) {
      return res.status(400).json({ sucesso: false, mensagem: 'Nome, email e senha são obrigatórios' });
    }

    if (senha.length < 6) {
      return res.status(400).json({ sucesso: false, mensagem: 'Senha deve ter no mínimo 6 caracteres' });
    }

    const existeUsuario = await User.findOne({ email });
    if (existeUsuario) {
      return res.status(400).json({ sucesso: false, mensagem: 'Email já cadastrado' });
    }

    const usuario = await User.create({ nome, email, senha, provider: 'local' });
    const token = signToken(usuario._id, usuario.role);

    res.status(201).json({
      sucesso: true,
      mensagem: 'Usuário criado com sucesso',
      token,
      usuario: { id: usuario._id, nome: usuario.nome, email: usuario.email, role: usuario.role },
    });
  } catch (err) {
    console.error('Erro no register:', err);
    const msg = mongoose.connection.readyState !== 1
      ? 'Banco de dados não conectado. Inicie o MongoDB.'
      : (err.message || 'Erro ao registrar');
    res.status(500).json({ sucesso: false, mensagem: msg });
  }
});

// POST /api/auth/login
router.post('/login', authLimiter, async (req, res) => {
  try {
    const { email, senha } = req.body;

    if (!email || !senha) {
      return res.status(400).json({ sucesso: false, mensagem: 'Email e senha são obrigatórios' });
    }

    const usuario = await User.findOne({ email }).select('+senha');
    if (!usuario || !usuario.senha) {
      return res.status(401).json({ sucesso: false, mensagem: 'Credenciais inválidas' });
    }

    const senhaValida = await usuario.compararSenha(senha);
    if (!senhaValida) {
      return res.status(401).json({ sucesso: false, mensagem: 'Credenciais inválidas' });
    }

    const token = signToken(usuario._id, usuario.role);

    res.json({
      sucesso: true,
      mensagem: 'Login realizado com sucesso',
      token,
      usuario: { id: usuario._id, nome: usuario.nome, email: usuario.email, role: usuario.role },
    });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message || 'Erro ao fazer login' });
  }
});

// GET /api/auth/me - Rota protegida
router.get('/me', authMiddleware, async (req, res) => {
  try {
    const usuario = await User.findById(req.userId);
    if (!usuario) {
      return res.status(404).json({ sucesso: false, mensagem: 'Usuário não encontrado' });
    }
    res.json({
      sucesso: true,
      usuario: { id: usuario._id, nome: usuario.nome, email: usuario.email, provider: usuario.provider, role: usuario.role },
    });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message || 'Erro ao buscar usuário' });
  }
});

// GET /api/auth/google
router.get('/google', (req, res, next) => {
  console.log('[Google OAuth] Iniciando fluxo, session id:', req.sessionID);
  passport.authenticate('google', { scope: ['profile', 'email'] })(req, res, next);
});

// GET /api/auth/google/callback
router.get('/google/callback', (req, res) => {
  console.log('[Google Callback] Recebido — query:', req.query);
  console.log('[Google Callback] Session id:', req.sessionID);
  console.log('[Google Callback] Session data:', JSON.stringify(req.session));

  passport.authenticate('google', (err, user, info) => {
    console.log('[Google Callback] err:', err?.message ?? null);
    console.log('[Google Callback] user:', user ? user.email : null);
    console.log('[Google Callback] info:', info);

    if (err || !user) {
      console.error('[Google Callback] FALHA —', err?.message || info?.message || 'sem usuário retornado');
      return res.redirect(`${getFrontendUrl()}/?erro=oauth`);
    }

    const token = signToken(user._id, user.role);
    console.log('[Google Callback] Sucesso → redirecionando para frontend');
    res.redirect(`${getFrontendUrl()}/auth/callback?token=${token}`);
  })(req, res);
});

export default router;
