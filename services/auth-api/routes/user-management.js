import express from 'express';
import authMiddleware, { gestaoMiddleware } from '../middleware/auth.js';
import { podeGerenciarOcupantes, podeGerenciarUsuarios } from '../constants/perfis.js';
import {
  listarUsuariosEscopo,
  emitirConvite,
  reenviarConvite,
  desativarUsuario,
} from '../services/userManagementService.js';
import {
  listarOcupantesUnidade,
  cadastrarLessee,
  cadastrarOccupant,
  cadastrarGuest,
  transferirResponsabilidadeFinanceira,
  verificarElegibilidadeTransferencia,
  removerVinculo,
} from '../services/occupantService.js';
import { usuarioPublico } from '../utils/usuarioPublico.js';

const router = express.Router();

function occupantUnitMiddleware(req, res, next) {
  const { unidadeId } = req.params;
  const perfil = req.userPerfil;

  if (podeGerenciarOcupantes(perfil)) {
    if (req.user.unidadeId !== unidadeId) {
      return res.status(403).json({
        sucesso: false,
        mensagem: 'Você não tem permissão para acessar esta unidade.',
      });
    }
  } else if (!podeGerenciarUsuarios(perfil)) {
    return res.status(403).json({
      sucesso: false,
      mensagem: 'Você não tem permissão para acessar esta funcionalidade.',
    });
  }

  next();
}

router.use(authMiddleware, gestaoMiddleware);

router.get('/users', async (req, res) => {
  try {
    const { usuarios, convitesPendentes } = await listarUsuariosEscopo(req.user);

    res.json({
      sucesso: true,
      usuarios: usuarios.map(usuarioPublico),
      convitesPendentes: convitesPendentes.map((c) => ({
        id: c.id,
        email: c.email,
        perfil: c.perfil,
        unidadeId: c.unidadeId,
        status: c.status,
        expiresAt: c.expiresAt,
        createdAt: c.createdAt,
        expirado: new Date(c.expiresAt) < new Date(),
      })),
    });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.post('/invites', async (req, res) => {
  try {
    const { email, perfil, unidadeId, nomePrecadastro, cpfPrecadastro } = req.body;

    if (!email || !perfil) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'E-mail e perfil são obrigatórios.',
      });
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
    console.error('Erro emitir convite:', err);
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.post('/invites/:id/resend', async (req, res) => {
  try {
    const resultado = await reenviarConvite(Number(req.params.id), req.user.id);

    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }

    res.json({
      sucesso: true,
      mensagem: resultado.emailEnviado
        ? 'Convite reenviado com sucesso.'
        : 'Convite reenviado, mas o e-mail não pôde ser enviado.',
      emailEnviado: resultado.emailEnviado,
      avisoEmail: resultado.avisoEmail || null,
      convite: {
        id: resultado.convite.id,
        email: resultado.convite.email,
        codigo: resultado.convite.codigo,
        expiresAt: resultado.convite.expiresAt,
      },
    });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.patch('/users/:id/deactivate', async (req, res) => {
  try {
    const resultado = await desativarUsuario(req.user, Number(req.params.id));

    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }

    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.get('/units/:unidadeId/occupants', occupantUnitMiddleware, async (req, res) => {
  try {
    const resultado = await listarOcupantesUnidade(req.user, req.params.unidadeId);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.get('/units/:unidadeId/transfer-eligibility', occupantUnitMiddleware, async (req, res) => {
  try {
    const resultado = await verificarElegibilidadeTransferencia(req.user, req.params.unidadeId);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.post('/units/:unidadeId/occupants/lessee', occupantUnitMiddleware, async (req, res) => {
  try {
    const resultado = await cadastrarLessee(req.user, req.params.unidadeId, req.body);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.status(201).json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.post('/units/:unidadeId/occupants/occupant', occupantUnitMiddleware, async (req, res) => {
  try {
    const resultado = await cadastrarOccupant(req.user, req.params.unidadeId, req.body);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.status(201).json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.post('/units/:unidadeId/occupants/guest', occupantUnitMiddleware, async (req, res) => {
  try {
    const resultado = await cadastrarGuest(req.user, req.params.unidadeId, req.body);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.status(201).json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.post('/units/:unidadeId/transfer-financial-responsibility', occupantUnitMiddleware, async (req, res) => {
  try {
    const resultado = await transferirResponsabilidadeFinanceira(req.user, req.params.unidadeId);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.delete('/units/:unidadeId/occupants/:userId', occupantUnitMiddleware, async (req, res) => {
  try {
    const resultado = await removerVinculo(req.user, req.params.unidadeId, Number(req.params.userId));
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

export default router;
