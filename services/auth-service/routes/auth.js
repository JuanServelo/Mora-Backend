import express from 'express';
import passport from 'passport';
import rateLimit from 'express-rate-limit';
import multer from 'multer';
import authMiddleware from '../middleware/auth.js';
import { googleOAuthConfigurado } from '../utils/oauthConfig.js';
import {
  register,
  login,
  getMe,
  updateMe,
  uploadFoto,
  forgotPassword,
  resetPassword,
  googleDisponivel,
  googleInit,
  googleCallback,
  erroOAuthRedirect,
  loginComErroRedirect,
} from '../controllers/authController.js';

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

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 5 * 1024 * 1024 },
});

router.post('/register', authLimiter, register);
router.post('/login', authLimiter, login);
router.get('/me', authMiddleware, getMe);
router.put('/me', authMiddleware, updateMe);
router.post('/me/foto', authMiddleware, upload.single('foto'), uploadFoto);
router.post('/forgot-password', forgotLimiter, forgotPassword);
router.post('/reset-password/:token', authLimiter, resetPassword);

router.get('/google/disponivel', googleDisponivel);

router.get('/google', (req, res, next) => {
  if (!googleOAuthConfigurado()) {
    return res.redirect(`${process.env.FRONTEND_URL || 'http://localhost:5173'}/login?erro=oauth_nao_configurado`);
  }
  passport.authenticate('google', { scope: ['profile', 'email'] })(req, res, next);
});

router.get('/google/callback', (req, res) => {
  passport.authenticate('google', (err, user) => {
    if (err || !user) {
      if (err) console.error('Erro no callback Google OAuth:', err.message || err);
      const codigo = erroOAuthRedirect(err);
      return res.redirect(loginComErroRedirect(codigo));
    }
    req.user = user;
    googleCallback(req, res);
  })(req, res);
});

export default router;
