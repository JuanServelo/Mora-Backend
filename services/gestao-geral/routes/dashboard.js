import express from 'express';
import { autenticar, exigirAdminGeral } from '../middleware/auth.js';
import { montarDashboard, montarResumoCondominio } from '../services/dashboardService.js';

const router = express.Router();

router.use(autenticar);

/** Painel da plataforma: exclusivo do Admin Geral. */
router.get('/dashboard', exigirAdminGeral, async (req, res) => {
  const resultado = await montarDashboard(req.authorization);
  if (resultado.erro) {
    return res.status(resultado.status).json({ sucesso: false, mensagem: resultado.mensagem });
  }
  res.json(resultado);
});

/**
 * Resumo de um condomínio. Sem `exigirAdminGeral`: o auth-api já decide se o
 * solicitante pertence àquele condomínio, e duplicar a regra aqui faria a
 * autorização viver em dois lugares.
 */
router.get('/condominios/:id/resumo', async (req, res) => {
  const resultado = await montarResumoCondominio(req.params.id, req.authorization);
  if (resultado.erro) {
    return res.status(resultado.status).json({ sucesso: false, mensagem: resultado.mensagem });
  }
  res.json(resultado);
});

export default router;
