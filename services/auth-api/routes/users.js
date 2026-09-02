import express from 'express';
import User from '../models/User.js';
import authMiddleware, { adminMiddleware, gestaoMiddleware } from '../middleware/auth.js';
import { usuarioPublico } from '../utils/usuarioPublico.js';
import {
  emitirConvite,
  desativarUsuario,
  listarUsuariosEscopo,
} from '../services/userManagementService.js';
import { PERFIS, podeGerenciarUsuarios } from '../constants/perfis.js';

const router = express.Router();

router.get('/', authMiddleware, async (req, res) => {
  try {
    if (!podeGerenciarUsuarios(req.userPerfil)) {
      return res.status(403).json({ sucesso: false, mensagem: 'Acesso negado' });
    }

    // Delega ao escopo já usado em /api/user-management: global para o Admin
    // Geral, do condomínio para a gestão, da unidade para morador e dono.
    // Antes esta rota fazia findAll sem filtro e devolvia a plataforma inteira.
    const { usuarios } = await listarUsuariosEscopo(req.user, {
      condominioId: req.query.condominioId,
    });
    res.json({ sucesso: true, usuarios: usuarios.map(usuarioPublico) });
  } catch (err) {
    console.error('Erro em /api/users:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao processar a solicitação' });
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
    console.error('Erro em /api/users:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao processar a solicitação' });
  }
});

router.put('/:id', authMiddleware, adminMiddleware, async (req, res) => {
  try {
    const { nome, email, senha } = req.body;

    const usuario = await User.scope('withPassword').findByPk(req.params.id);
    if (!usuario) {
      return res.status(404).json({ sucesso: false, mensagem: 'Usuário não encontrado' });
    }

    // Fora do Admin Geral, só se edita quem é do próprio condomínio. Responde
    // 404 em vez de 403: quem não pode ver não deve descobrir que existe.
    const ehAdminGeral = req.userPerfil === PERFIS.ADMIN_GERAL;
    if (!ehAdminGeral && usuario.condominioId !== req.user.condominioId) {
      return res.status(404).json({ sucesso: false, mensagem: 'Usuário não encontrado' });
    }

    if (nome) usuario.nome = nome;
    if (email) usuario.email = email;
    if (senha) usuario.senha = senha;

    await usuario.save();

    res.json({ sucesso: true, usuario: usuarioPublico(usuario) });
  } catch (err) {
    console.error('Erro em /api/users:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao processar a solicitação' });
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
    console.error('Erro em /api/users:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao processar a solicitação' });
  }
});

export default router;
