import express from 'express';
import authMiddleware, { adminMiddleware } from '../middleware/auth.js';
import { PERFIS_DESCRICAO, CATEGORIAS_PERFIS } from '../constants/perfisInfo.js';
import { PERFIS } from '../constants/perfis.js';

const router = express.Router();

router.use(authMiddleware, adminMiddleware);

// Informações completas sobre todos os perfis (para admins)
router.get('/info', (req, res) => {
  const perfis = Object.entries(PERFIS_DESCRICAO).map(([chave, info]) => ({
    chave,
    ...info,
    podeCadastrarLabels: (info.podeCadastrar || []).map((p) => PERFIS_DESCRICAO[p]?.label || p),
  }));

  const porCategoria = Object.entries(CATEGORIAS_PERFIS).map(([cat, meta]) => ({
    categoria: cat,
    ...meta,
    perfis: perfis.filter((p) => p.categoria === cat),
  }));

  res.json({
    sucesso: true,
    categorias: porCategoria,
    total: perfis.length,
  });
});

// Resumo rápido de um perfil específico
router.get('/info/:perfil', (req, res) => {
  const { perfil } = req.params;
  const info = PERFIS_DESCRICAO[perfil];

  if (!info) {
    return res.status(404).json({ sucesso: false, mensagem: 'Perfil não encontrado.' });
  }

  res.json({
    sucesso: true,
    perfil: {
      chave: perfil,
      ...info,
      podeCadastrarLabels: (info.podeCadastrar || []).map((p) => PERFIS_DESCRICAO[p]?.label || p),
    },
  });
});

export default router;
