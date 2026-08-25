import { PERFIS_DESCRICAO, CATEGORIAS_PERFIS } from '../constants/perfisInfo.js';

export const listAllPerfisInfo = (req, res) => {
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
};

export const getPerfilInfo = (req, res) => {
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
};
