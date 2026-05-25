import express from 'express';
import User from '../models/User.js';
import authMiddleware, { adminMiddleware, gestaoMiddleware } from '../middleware/auth.js';
import { usuarioPublico, normalizarVinculo } from '../utils/usuarioPublico.js';
import { emitirConvite } from '../services/userManagementService.js';
import { desativarUsuario } from '../services/userManagementService.js';
import { podeGerenciarUsuarios } from '../constants/perfis.js';

const router = express.Router();

router.get('/', authMiddleware, async (req, res) => {
  try {
    if (!podeGerenciarUsuarios(req.userPerfil)) {
      return res.status(403).json({ sucesso: false, mensagem: 'Acesso negado' });
    }

    const usuarios = await User.findAll({ order: [['createdAt', 'DESC']] });
    res.json({ sucesso: true, usuarios: usuarios.map(usuarioPublico) });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.post('/', authMiddleware, gestaoMiddleware, async (req, res) => {
  try {
    const { email, perfil, unidadeId, nomePrecadastro, cpfPrecadastro } = req.body;

    if (!email || !perfil) {
      return res.status(400).json({ sucesso: false, mensagem: 'E-mail e perfil são obrigatórios.' });
    }

    const resultado = await emitirConvite(req.user, {
      email,
      perfil,
      unidadeId,
      nomePrecadastro,
      cpfPrecadastro,
    });

    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }

    res.status(201).json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.put('/:id', authMiddleware, adminMiddleware, async (req, res) => {
  try {
    const { nome, email, senha, bloco, apartamento, vaga, perfil } = req.body;

    const usuario = await User.scope('withPassword').findByPk(req.params.id);
    if (!usuario) {
      return res.status(404).json({ sucesso: false, mensagem: 'Usuário não encontrado' });
    }

    if (nome) usuario.nome = nome;
    if (email) usuario.email = email;
    if (perfil) usuario.perfil = perfil;
    if (senha) usuario.senha = senha;
    if (bloco !== undefined) usuario.bloco = normalizarVinculo(bloco);
    if (apartamento !== undefined) usuario.apartamento = normalizarVinculo(apartamento);
    if (vaga !== undefined) usuario.vaga = normalizarVinculo(vaga);

    await usuario.save();

    res.json({ sucesso: true, usuario: usuarioPublico(usuario) });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.delete('/:id', authMiddleware, gestaoMiddleware, async (req, res) => {
  try {
    if (req.params.id === req.userId?.toString()) {
      return res.status(400).json({ sucesso: false, mensagem: 'Não é possível desativar sua própria conta' });
    }

    const resultado = await desativarUsuario(req.user, Number(req.params.id));
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }

    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

export default router;
