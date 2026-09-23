import express from 'express';
import Reclamacao from '../models/Reclamacao.js';
import User from '../models/User.js';
import authMiddleware, { adminMiddleware } from '../middleware/auth.js';
import { PERFIS } from '../constants/perfis.js';

const router = express.Router();

function gerarProtocolo() {
  const t = Date.now().toString(36).toUpperCase();
  const r = Math.random().toString(36).slice(2, 6).toUpperCase();
  return `REC-${t}-${r}`;
}

/**
 * Escopo de leitura do ator.
 *
 * O Admin Geral opera a plataforma e enxerga todos os condomínios; os demais
 * perfis administrativos ficam restritos ao próprio. Sem isso um síndico lia e
 * respondia ocorrências de outros clientes.
 */
function escopoDoAtor(req) {
  if (req.userPerfil === PERFIS.ADMIN_GERAL) {
    return req.query.condominioId ? { condominioId: req.query.condominioId } : {};
  }
  return { condominioId: req.user.condominioId };
}

function serializar(rec, includeEmail = false) {
  const base = {
    id: rec.id,
    protocolNumber: rec.protocolNumber,
    category: rec.category,
    description: rec.description,
    attachmentUrl: rec.attachmentUrl,
    status: rec.status,
    condominioId: rec.condominioId,
    createdAt: rec.createdAt,
    interactions: Array.isArray(rec.interactions) ? rec.interactions : [],
  };
  if (includeEmail && rec.usuario) {
    return {
      ...base,
      residentId: rec.usuario?.email ?? String(rec.userId),
      usuario: { nome: rec.usuario?.nome, email: rec.usuario?.email },
    };
  }
  return base;
}

// POST /api/reclamacoes — morador cria
router.post('/', authMiddleware, async (req, res) => {
  try {
    const { category, description, attachmentUrl } = req.body;
    if (!category || !description) {
      return res.status(400).json({ sucesso: false, mensagem: 'Categoria e descrição são obrigatórias' });
    }

    let protocolNumber = gerarProtocolo();
    for (let i = 0; i < 5; i += 1) {
      const existe = await Reclamacao.findOne({ where: { protocolNumber } });
      if (!existe) break;
      protocolNumber = gerarProtocolo();
    }

    const rec = await Reclamacao.create({
      userId: req.userId,
      protocolNumber,
      category,
      description,
      attachmentUrl: attachmentUrl || null,
      // Herda o condomínio de quem abriu: é o que delimita quem pode atender.
      condominioId: req.user.condominioId,
      interactions: [],
    });

    const criada = await Reclamacao.findByPk(rec.id);
    res.status(201).json({ sucesso: true, reclamacao: serializar(criada) });
  } catch (err) {
    console.error('Erro ao registrar reclamação:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao registrar reclamação' });
  }
});

// GET /api/reclamacoes/minhas — lista do usuário logado
router.get('/minhas', authMiddleware, async (req, res) => {
  try {
    const lista = await Reclamacao.findAll({
      where: { userId: req.userId },
      order: [['createdAt', 'DESC']],
    });
    res.json({
      sucesso: true,
      reclamacoes: lista.map((r) => serializar(r)),
    });
  } catch (err) {
    console.error('Erro ao listar reclamações do usuário:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao listar reclamações' });
  }
});

// GET /api/reclamacoes/todas — gestão, restrita ao condomínio do ator
router.get('/todas', authMiddleware, adminMiddleware, async (req, res) => {
  try {
    const lista = await Reclamacao.findAll({
      where: escopoDoAtor(req),
      include: [{ model: User, as: 'usuario', attributes: ['id', 'nome', 'email'] }],
      order: [['createdAt', 'DESC']],
    });
    res.json({
      sucesso: true,
      reclamacoes: lista.map((r) => serializar(r, true)),
    });
  } catch (err) {
    console.error('Erro ao listar reclamações:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao listar reclamações' });
  }
});

// PATCH /api/reclamacoes/:id — gestão responde ou altera o status
router.patch('/:id', authMiddleware, adminMiddleware, async (req, res) => {
  try {
    const { status, response } = req.body;
    const rec = await Reclamacao.findByPk(req.params.id);
    if (!rec) {
      return res.status(404).json({ sucesso: false, mensagem: 'Reclamação não encontrada' });
    }

    // Responde 404, não 403: quem não pode ver não deve descobrir que existe.
    const ehAdminGeral = req.userPerfil === PERFIS.ADMIN_GERAL;
    if (!ehAdminGeral && rec.condominioId !== req.user.condominioId) {
      return res.status(404).json({ sucesso: false, mensagem: 'Reclamação não encontrada' });
    }

    if (status && ['PENDENTE', 'EM_ANALISE', 'RESOLVIDO'].includes(status)) {
      rec.status = status;
    }

    if (response && String(response).trim()) {
      const interactions = Array.isArray(rec.interactions) ? [...rec.interactions] : [];
      interactions.push({
        id: Date.now(),
        response: String(response).trim(),
        status: rec.status,
        createdAt: new Date().toISOString(),
      });
      rec.interactions = interactions;
    }

    await rec.save();
    const atualizada = await Reclamacao.findByPk(rec.id, {
      include: [{ model: User, as: 'usuario', attributes: ['id', 'nome', 'email'] }],
    });
    res.json({ sucesso: true, reclamacao: serializar(atualizada, true) });
  } catch (err) {
    console.error('Erro ao atualizar reclamação:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao atualizar reclamação' });
  }
});

export default router;
