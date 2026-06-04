import crypto from 'crypto';
import express from 'express';
import jwt from 'jsonwebtoken';
import passport from 'passport';
import rateLimit from 'express-rate-limit';
import multer from 'multer';
import sharp from 'sharp';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';
import { Op } from 'sequelize';
import User from '../models/User.js';
import authMiddleware from '../middleware/auth.js';
import { usuarioPublico } from '../utils/usuarioPublico.js';
import { enviarEmailReset, emailConfigurado } from '../utils/emailService.js';
import { googleOAuthConfigurado } from '../utils/oauthConfig.js';
import { validarSenha } from '../utils/passwordValidation.js';
import { redirectPorPerfil } from '../utils/redirectPorPerfil.js';
import { STATUS_USUARIO } from '../constants/perfis.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const UPLOAD_DIR = process.env.STORAGE_PATH || path.join(__dirname, '..', 'uploads', 'avatars');

if (!fs.existsSync(UPLOAD_DIR)) {
  fs.mkdirSync(UPLOAD_DIR, { recursive: true });
}

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

const getJwtSecret = () => process.env.JWT_SECRET;
const getFrontendUrl = () => process.env.FRONTEND_URL || 'http://localhost:5173';

function erroOAuthRedirect(err) {
  const msg = err?.message || '';
  if (msg.includes('Conta não encontrada')) return 'sem-convite';
  if (msg.includes('Cadastro pendente')) return 'cadastro_pendente';
  if (msg.includes('Conta desativada')) return 'conta_desativada';
  return 'oauth';
}

function loginComErroRedirect(codigo, extra = '') {
  if (codigo === 'sem-convite') {
    return `${getFrontendUrl()}/sem-convite?origem=google${extra}`;
  }
  return `${getFrontendUrl()}/login?erro=${codigo}${extra}`;
}

export const signToken = (userId, perfil, tokenVersion = 0) =>
  jwt.sign({ id: userId, perfil, tokenVersion }, getJwtSecret(), { expiresIn: '7d' });

router.post('/register', authLimiter, (_req, res) => {
  res.status(403).json({
    sucesso: false,
    mensagem: 'Cadastro disponível apenas via código de convite. Acesse a página de ativação.',
  });
});

router.post('/login', authLimiter, async (req, res) => {
  try {
    const { email, senha } = req.body;

    if (!email || !senha) {
      return res.status(400).json({ sucesso: false, mensagem: 'Email e senha são obrigatórios' });
    }

    const usuario = await User.scope('withPassword').findOne({
      where: { email: email.toLowerCase().trim() },
    });

    if (!usuario || !usuario.senha) {
      return res.status(401).json({ sucesso: false, mensagem: 'E-mail ou senha inválidos.' });
    }

    if (usuario.status === STATUS_USUARIO.INACTIVE) {
      return res.status(401).json({ sucesso: false, mensagem: 'Esta conta foi desativada.' });
    }

    if (usuario.status === STATUS_USUARIO.PENDING_ACTIVATION) {
      return res.status(401).json({
        sucesso: false,
        mensagem: 'Seu cadastro ainda não foi concluído. Acesse o link enviado para seu e-mail para criar sua senha.',
      });
    }

    if (usuario.semAcessoSistema) {
      return res.status(401).json({ sucesso: false, mensagem: 'Guests não possuem acesso ao sistema.' });
    }

    const senhaValida = await usuario.compararSenha(senha);
    if (!senhaValida) {
      return res.status(401).json({ sucesso: false, mensagem: 'E-mail ou senha inválidos.' });
    }

    const perfil = usuario.getPerfilEfetivo();
    const token = signToken(usuario.id, perfil, usuario.tokenVersion || 0);

    res.json({
      sucesso: true,
      mensagem: 'Login realizado com sucesso',
      token,
      usuario: usuarioPublico(usuario),
      redirectPath: redirectPorPerfil(perfil),
    });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message || 'Erro ao fazer login' });
  }
});

router.get('/me', authMiddleware, async (req, res) => {
  try {
    res.json({
      sucesso: true,
      usuario: usuarioPublico(req.user),
    });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message || 'Erro ao buscar usuário' });
  }
});

router.put('/me', authMiddleware, async (req, res) => {
  try {
    const { nome, email, telefone, senha, senhaAtual } = req.body;
    const usuario = await User.scope('withPassword').findByPk(req.userId);
    if (!usuario) {
      return res.status(404).json({ sucesso: false, mensagem: 'Usuário não encontrado' });
    }

    const erros = {};
    if (nome !== undefined && (!nome || !String(nome).trim())) {
      erros.nome = 'Este campo é obrigatório.';
    }
    if (email !== undefined && (!email || !String(email).trim())) {
      erros.email = 'Este campo é obrigatório.';
    }
    if (Object.keys(erros).length > 0) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'Este campo é obrigatório.',
        erros,
      });
    }

    if (nome) usuario.nome = nome.trim();
    if (telefone !== undefined) usuario.telefone = telefone?.trim() || null;
    if (email) {
      const emailNorm = email.toLowerCase().trim();
      const duplicado = await User.findOne({
        where: {
          email: emailNorm,
          id: { [Op.ne]: usuario.id },
        },
      });
      if (duplicado) {
        return res.status(400).json({
          sucesso: false,
          mensagem: 'Este e-mail já está em uso por outro perfil.',
        });
      }
      usuario.email = emailNorm;
    }

    if (senha) {
      const errosSenha = validarSenha(senha);
      if (errosSenha.length > 0) {
        return res.status(400).json({ sucesso: false, mensagem: errosSenha[0], errosSenha });
      }
      if (senhaAtual) {
        const ok = await usuario.compararSenha(senhaAtual);
        if (!ok) {
          return res.status(400).json({ sucesso: false, mensagem: 'Senha atual incorreta.' });
        }
      }
      usuario.senha = senha;
    }

    await usuario.save();
    res.json({
      sucesso: true,
      mensagem: 'Dados atualizados com sucesso.',
      usuario: usuarioPublico(usuario),
    });
  } catch (err) {
    if (err.name === 'SequelizeUniqueConstraintError') {
      return res.status(400).json({ sucesso: false, mensagem: 'Este e-mail já está em uso por outro perfil.' });
    }
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao atualizar perfil' });
  }
});

router.post('/me/foto', authMiddleware, upload.single('foto'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ sucesso: false, mensagem: 'Nenhum arquivo enviado.' });
    }

    const mime = req.file.mimetype?.toLowerCase();
    if (!['image/jpeg', 'image/jpg', 'image/png'].includes(mime)) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'Formato não permitido. Envie uma imagem JPG ou PNG.',
      });
    }

    const usuario = await User.findByPk(req.userId);
    const filename = `avatar-${usuario.id}-${Date.now()}.jpg`;
    const filepath = path.join(UPLOAD_DIR, filename);

    await sharp(req.file.buffer)
      .resize(256, 256, { fit: 'cover' })
      .jpeg({ quality: 85 })
      .toFile(filepath);

    if (usuario.fotoUrl) {
      const oldPath = path.join(UPLOAD_DIR, path.basename(usuario.fotoUrl));
      if (fs.existsSync(oldPath)) fs.unlinkSync(oldPath);
    }

    usuario.fotoUrl = `/uploads/avatars/${filename}`;
    await usuario.save();

    res.json({
      sucesso: true,
      mensagem: 'Foto atualizada com sucesso.',
      usuario: usuarioPublico(usuario),
    });
  } catch (err) {
    console.error('Erro upload foto:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao enviar foto' });
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

    if (!emailConfigurado()) {
      return res.status(503).json({
        sucesso: false,
        mensagem: 'Serviço de e-mail não configurado. Defina MAIL_USER e MAIL_PASS no .env.',
      });
    }

    await enviarEmailReset(usuario.email, token);

    res.json({ sucesso: true, mensagem: 'Se esse email estiver cadastrado, você receberá um link em breve.' });
  } catch (err) {
    console.error('Erro no forgot-password:', err);
    if (err.message?.includes('e-mail não configurado')) {
      return res.status(503).json({ sucesso: false, mensagem: err.message });
    }
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao processar solicitação' });
  }
});

router.post('/reset-password/:token', authLimiter, async (req, res) => {
  try {
    const { senha } = req.body;
    const errosSenha = validarSenha(senha);
    if (errosSenha.length > 0) {
      return res.status(400).json({ sucesso: false, mensagem: errosSenha[0], errosSenha });
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

router.get('/google/disponivel', (_req, res) => {
  res.json({ disponivel: googleOAuthConfigurado() });
});

router.get('/google', (req, res, next) => {
  if (!googleOAuthConfigurado()) {
    return res.redirect(`${getFrontendUrl()}/login?erro=oauth_nao_configurado`);
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

    if (user.status === STATUS_USUARIO.INACTIVE) {
      return res.redirect(`${getFrontendUrl()}/login?erro=conta_desativada`);
    }
    if (user.status === STATUS_USUARIO.PENDING_ACTIVATION) {
      return res.redirect(`${getFrontendUrl()}/login?erro=cadastro_pendente`);
    }
    if (user.semAcessoSistema) {
      return res.redirect(`${getFrontendUrl()}/login?erro=sem_acesso_sistema`);
    }

    const perfil = user.getPerfilEfetivo();
    const token = signToken(user.id, perfil, user.tokenVersion || 0);
    res.redirect(`${getFrontendUrl()}/auth/callback?token=${token}`);
  })(req, res);
});

export default router;
