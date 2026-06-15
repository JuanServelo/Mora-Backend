import express from 'express';
import authMiddleware, { superAdminMiddleware } from '../middleware/auth.js';
import {
  listarPlanos,
  buscarPlano,
  criarPlano,
  editarPlano,
  definirStatusPlano,
  configurarModulos,
} from '../services/planService.js';
import { MODULOS_VALIDOS } from '../constants/perfis.js';

const router = express.Router();

router.use(authMiddleware, superAdminMiddleware);

/** Catálogo de módulos disponíveis (apoio ao cadastro de planos). */
router.get('/modulos', (_req, res) => {
  res.json({ sucesso: true, modulos: MODULOS_VALIDOS });
});

// US-01.5 — Consulta de planos
router.get('/', async (_req, res) => {
  try {
    const resultado = await listarPlanos();
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.get('/:id', async (req, res) => {
  try {
    const resultado = await buscarPlano(Number(req.params.id));
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// US-01.1 — Cadastro de plano
router.post('/', async (req, res) => {
  try {
    const resultado = await criarPlano(req.body);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.status(201).json(resultado);
  } catch (err) {
    console.error('Erro criar plano:', err);
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// US-01.2 — Edição de plano
router.put('/:id', async (req, res) => {
  try {
    const resultado = await editarPlano(Number(req.params.id), req.body);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    console.error('Erro editar plano:', err);
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// US-01.3 — Ativação / desativação de plano
router.patch('/:id/activate', async (req, res) => {
  try {
    const resultado = await definirStatusPlano(Number(req.params.id), true);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

router.patch('/:id/deactivate', async (req, res) => {
  try {
    const resultado = await definirStatusPlano(Number(req.params.id), false);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// US-01.4 — Configuração de módulos do plano
router.put('/:id/modules', async (req, res) => {
  try {
    const resultado = await configurarModulos(Number(req.params.id), req.body.activeModules);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

export default router;
