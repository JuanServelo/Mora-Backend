import Reclamacao from '../models/Reclamacao.js';
import User from '../models/User.js';

function gerarProtocolo() {
  const t = Date.now().toString(36).toUpperCase();
  const r = Math.random().toString(36).slice(2, 6).toUpperCase();
  return `REC-${t}-${r}`;
}

function serializar(rec, includeEmail = false) {
  const base = {
    id: rec.id,
    protocolNumber: rec.protocolNumber,
    category: rec.category,
    description: rec.description,
    attachmentUrl: rec.attachmentUrl,
    status: rec.status,
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

export const create = async (req, res) => {
  try {
    const { category, description, attachmentUrl } = req.body;
    if (!category || !description) {
      return res.status(400).json({ sucesso: false, mensagem: 'Categoria e descrição são obrigatórias' });
    }

    let protocolNumber = gerarProtocolo();
    for (let i = 0; i < 5; i++) {
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
      interactions: [],
    });

    const criada = await Reclamacao.findByPk(rec.id);
    res.status(201).json({ sucesso: true, reclamacao: serializar(criada) });
  } catch (err) {
    console.error(err);
    res.status(500).json({ sucesso: false, mensagem: err.message || 'Erro ao registrar reclamação' });
  }
};

export const listMine = async (req, res) => {
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
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
};

export const listAll = async (req, res) => {
  try {
    const lista = await Reclamacao.findAll({
      include: [{ model: User, as: 'usuario', attributes: ['id', 'nome', 'email'] }],
      order: [['createdAt', 'DESC']],
    });
    res.json({
      sucesso: true,
      reclamacoes: lista.map((r) => serializar(r, true)),
    });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
};

export const update = async (req, res) => {
  try {
    const { status, response } = req.body;
    const rec = await Reclamacao.findByPk(req.params.id);
    if (!rec) {
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
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
};
