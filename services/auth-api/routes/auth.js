import express from 'express';
import jwt from 'jsonwebtoken';
import mongoose from 'mongoose';
import User from '../models/User.js';
import authMiddleware from '../middleware/auth.js';

const router = express.Router();
const JWT_SECRET = process.env.JWT_SECRET || 'sua-chave-secreta-mudar-em-producao';

// POST /api/auth/register
router.post('/register', async (req, res) => {
  try {
    const { nome, email, senha } = req.body;

    if (!nome || !email || !senha) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'Nome, email e senha são obrigatórios',
      });
    }

    const existeUsuario = await User.findOne({ email });
    if (existeUsuario) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'Email já cadastrado',
      });
    }

    const usuario = await User.create({ nome, email, senha });
    const token = jwt.sign(
      { id: usuario._id },
      JWT_SECRET,
      { expiresIn: '7d' }
    );

    res.status(201).json({
      sucesso: true,
      mensagem: 'Usuário criado com sucesso',
      token,
      usuario: {
        id: usuario._id,
        nome: usuario.nome,
        email: usuario.email,
      },
    });
  } catch (err) {
    console.error('Erro no register:', err);
    const msg = mongoose.connection.readyState !== 1
      ? 'Banco de dados não conectado. Inicie o MongoDB.'
      : (err.message || 'Erro ao registrar');
    res.status(500).json({
      sucesso: false,
      mensagem: msg,
    });
  }
});

// POST /api/auth/login
router.post('/login', async (req, res) => {
  try {
    const { email, senha } = req.body;

    if (!email || !senha) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'Email e senha são obrigatórios',
      });
    }

    const usuario = await User.findOne({ email }).select('+senha');
    if (!usuario) {
      return res.status(401).json({
        sucesso: false,
        mensagem: 'Credenciais inválidas',
      });
    }

    const senhaValida = await usuario.compararSenha(senha);
    if (!senhaValida) {
      return res.status(401).json({
        sucesso: false,
        mensagem: 'Credenciais inválidas',
      });
    }

    const token = jwt.sign(
      { id: usuario._id },
      JWT_SECRET,
      { expiresIn: '7d' }
    );

    res.json({
      sucesso: true,
      mensagem: 'Login realizado com sucesso',
      token,
      usuario: {
        id: usuario._id,
        nome: usuario.nome,
        email: usuario.email,
      },
    });
  } catch (err) {
    res.status(500).json({
      sucesso: false,
      mensagem: err.message || 'Erro ao fazer login',
    });
  }
});

// GET /api/auth/me - Rota protegida
router.get('/me', authMiddleware, async (req, res) => {
  try {
    const usuario = await User.findById(req.userId);
    if (!usuario) {
      return res.status(404).json({
        sucesso: false,
        mensagem: 'Usuário não encontrado',
      });
    }
    res.json({
      sucesso: true,
      usuario: {
        id: usuario._id,
        nome: usuario.nome,
        email: usuario.email,
      },
    });
  } catch (err) {
    res.status(500).json({
      sucesso: false,
      mensagem: err.message || 'Erro ao buscar usuário',
    });
  }
});

export default router;
