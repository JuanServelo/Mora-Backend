import express from 'express';
import { autenticar, exigirPerfis, PERFIS_GESTAO, PERFIS_UNIDADE } from '../middleware/auth.js';
import { resolverEscopo, resolverUnidade, exigirEscrita } from '../middleware/escopo.js';
import * as controller from '../controllers/multasController.js';
import { embrulhar } from '../utils/assincrono.js';

const ctrl = embrulhar(controller);
const router = express.Router();

// O Admin Geral acompanha, mas quem aplica e julga é o síndico: a multa é ato
// da administração do condomínio, como o fechamento e a baixa manual.
const gestao = [autenticar, exigirPerfis(...PERFIS_GESTAO), resolverEscopo];
const sindico = [autenticar, exigirPerfis('ADMIN_SINDICO'), resolverEscopo, exigirEscrita];
const unidade = [autenticar, exigirPerfis(...PERFIS_UNIDADE), resolverEscopo, resolverUnidade];

router.get('/admin/multas', gestao, ctrl.listar);
router.post('/admin/multas', sindico, ctrl.aplicar);
router.patch('/admin/multas/:id/julgar', sindico, ctrl.julgar);
router.patch('/admin/multas/:id/cancelar', sindico, ctrl.cancelar);

router.get('/multas/minhas', unidade, ctrl.minhas);
router.post('/multas/:id/recurso', unidade, ctrl.recorrer);

export default router;
