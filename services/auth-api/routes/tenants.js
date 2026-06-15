import express from 'express';
import authMiddleware, { superAdminMiddleware } from '../middleware/auth.js';
import {
  listarTenants,
  buscarTenant,
  criarTenant,
  editarTenant,
  provisionarTenant,
  suspenderTenant,
  reativarTenant,
  alterarPlanoTenant,
} from '../services/tenantService.js';
import { TIPOS_TENANT_VALUES } from '../constants/perfis.js';

const router = express.Router();

router.use(authMiddleware, superAdminMiddleware);

/** Tipos de tenant disponíveis (apoio ao cadastro). */
router.get('/tipos', (_req, res) => {
  res.json({ sucesso: true, tipos: TIPOS_TENANT_VALUES });
});

// US-02.6 — Consulta (listagem)
router.get('/', async (_req, res) => {
  try {
    const resultado = await listarTenants();
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// US-02.6 — Consulta de configuração do tenant
router.get('/:id', async (req, res) => {
  try {
    const resultado = await buscarTenant(Number(req.params.id));
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// US-02.1 — Cadastro de novo tenant
router.post('/', async (req, res) => {
  try {
    const resultado = await criarTenant(req.body);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.status(201).json(resultado);
  } catch (err) {
    console.error('Erro criar tenant:', err);
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// US-02.2 — Edição de tenant
router.put('/:id', async (req, res) => {
  try {
    const resultado = await editarTenant(Number(req.params.id), req.body);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    console.error('Erro editar tenant:', err);
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// US-02.3 — Provisionamento inicial do tenant
router.post('/:id/provision', async (req, res) => {
  try {
    const resultado = await provisionarTenant(Number(req.params.id));
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    console.error('Erro provisionar tenant:', err);
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// US-02.4 — Suspensão de tenant
router.patch('/:id/suspend', async (req, res) => {
  try {
    const resultado = await suspenderTenant(Number(req.params.id));
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// US-02.4 (complemento) — Reativação de tenant
router.patch('/:id/reactivate', async (req, res) => {
  try {
    const resultado = await reativarTenant(Number(req.params.id));
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// US-02.5 — Gerenciamento de plano do tenant
router.patch('/:id/plan', async (req, res) => {
  try {
    const resultado = await alterarPlanoTenant(Number(req.params.id), req.body.planId);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

export default router;
