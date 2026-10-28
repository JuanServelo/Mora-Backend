import express from 'express';
import { autenticar, exigirPerfis } from '../middleware/auth.js';
import * as ctrl from '../controllers/plataformaController.js';
import { protegerRotas } from '../utils/assincrono.js';

const router = express.Router();

// ── Admin Geral: a mensalidade do Mora em cobrança recorrente ───────────────

const adminGeral = [autenticar, exigirPerfis('ADMIN_GERAL')];

router.post('/plataforma/assinaturas', ...adminGeral, ctrl.ativar);
router.get('/plataforma/assinaturas', ...adminGeral, ctrl.listar);
router.patch('/plataforma/assinaturas/:id/cancelar', ...adminGeral, ctrl.cancelar);
router.post('/plataforma/pagamentos/:id/estornar', ...adminGeral, ctrl.estornarPagamento);

// ── Síndico: a do próprio condomínio ────────────────────────────────────────

router.get('/plataforma/minha-assinatura', autenticar, exigirPerfis('ADMIN_SINDICO'), ctrl.minhaAssinatura);

// Erro num handler async vira 500 com mensagem genérica, em vez de deixar a
// requisição sem resposta e derrubar o processo (Express 4).
export default protegerRotas(router);
