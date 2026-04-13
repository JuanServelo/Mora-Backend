import express from 'express';
import User from '../models/User.js';
import authMiddleware, { adminMiddleware } from '../middleware/auth.js';

const router = express.Router();

router.use(authMiddleware, adminMiddleware);

// GET /api/users
router.get('/', async (req, res) => {
  try {
    const usuarios = await User.findAll({ order: [['createdAt', 'DESC']] });
    res.json({ sucesso: true, usuarios });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// POST /api/users
router.post('/', async (req, res) => {
  try {
    const { nome, email, senha, role } = req.body;

    if (!nome || !email || !senha) {
      return res.status(400).json({ sucesso: false, mensagem: 'Nome, email e senha são obrigatórios' });
    }
    if (senha.length < 6) {
      return res.status(400).json({ sucesso: false, mensagem: 'Senha deve ter no mínimo 6 caracteres' });
    }

    const existe = await User.findOne({ where: { email } });
    if (existe) {
      return res.status(400).json({ sucesso: false, mensagem: 'Email já cadastrado' });
    }

    const usuario = await User.create({ nome, email, senha, role: role || 'user', provider: 'local' });
    res.status(201).json({
      sucesso: true,
      usuario: { id: usuario.id, nome: usuario.nome, email: usuario.email, role: usuario.role, provider: usuario.provider, createdAt: usuario.createdAt },
    });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// PUT /api/users/:id
router.put('/:id', async (req, res) => {
  try {
    const { nome, email, senha, role } = req.body;

    const usuario = await User.scope('withPassword').findByPk(req.params.id);
    if (!usuario) {
      return res.status(404).json({ sucesso: false, mensagem: 'Usuário não encontrado' });
    }

    if (nome) usuario.nome = nome;
    if (email) usuario.email = email;
    if (role) usuario.role = role;
    if (senha) {
      if (senha.length < 6) {
        return res.status(400).json({ sucesso: false, mensagem: 'Senha deve ter no mínimo 6 caracteres' });
      }
      usuario.senha = senha;
    }

    await usuario.save();
    res.json({
      sucesso: true,
      usuario: { id: usuario.id, nome: usuario.nome, email: usuario.email, role: usuario.role, provider: usuario.provider, createdAt: usuario.createdAt },
    });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// DELETE /api/users/:id
router.delete('/:id', async (req, res) => {
  try {
    if (req.params.id === req.userId?.toString()) {
      return res.status(400).json({ sucesso: false, mensagem: 'Não é possível deletar sua própria conta' });
    }

    const usuario = await User.findByPk(req.params.id);
    if (!usuario) {
      return res.status(404).json({ sucesso: false, mensagem: 'Usuário não encontrado' });
    }

    await usuario.destroy();
    res.json({ sucesso: true, mensagem: 'Usuário deletado com sucesso' });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

export default router;
