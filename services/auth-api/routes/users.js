import express from 'express';
import User from '../models/User.js';
import authMiddleware, { adminMiddleware } from '../middleware/auth.js';
import { usuarioPublico, normalizarVinculo } from '../utils/usuarioPublico.js';

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

    const { bloco, apartamento, vaga } = req.body;
    const usuario = await User.create({
      nome,
      email,
      senha,
      role: role || 'user',
      provider: 'local',
      bloco: normalizarVinculo(bloco) ?? null,
      apartamento: normalizarVinculo(apartamento) ?? null,
      vaga: normalizarVinculo(vaga) ?? null,
    });
    res.status(201).json({
      sucesso: true,
      usuario: usuarioPublico(usuario),
    });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// PUT /api/users/:id
router.put('/:id', async (req, res) => {
  try {
    const { nome, email, senha, role, bloco, apartamento, vaga } = req.body;

    console.log("REQ BODY:", req.body);

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
    if (bloco !== undefined) usuario.bloco = normalizarVinculo(bloco);
    if (apartamento !== undefined) usuario.apartamento = normalizarVinculo(apartamento);
    if (vaga !== undefined) usuario.vaga = normalizarVinculo(vaga);

    await usuario.save();
    const atualizado = await User.findByPk(req.params.id, {
    attributes: [
        'id',
        'nome',
        'email',
        'provider',
        'role',
        'bloco',
        'apartamento',
        'vaga',
        'createdAt'
      ]
    });

    res.json({
      sucesso: true,
      usuario: atualizado,
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
