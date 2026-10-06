import express from 'express';
import { autenticar, exigirPerfis, PERFIS_GESTAO, PERFIS_UNIDADE } from '../middleware/auth.js';
import { resolverEscopo, exigirEscrita } from '../middleware/escopo.js';
import * as controller from '../controllers/prestacaoController.js';
import { embrulhar } from '../utils/assincrono.js';

const ctrl = embrulhar(controller);
const router = express.Router();

const gestao = [autenticar, exigirPerfis(...PERFIS_GESTAO), resolverEscopo];
const sindico = [autenticar, exigirPerfis('ADMIN_SINDICO'), resolverEscopo, exigirEscrita];
// A prestação é do condomínio inteiro, não da unidade: basta o condomínio do
// token, sem perguntar a unidade ao auth-api.
const morador = [autenticar, exigirPerfis(...PERFIS_UNIDADE), resolverEscopo];

router.get('/admin/prestacao/:competencia', gestao, ctrl.obter);
router.post('/admin/prestacao/:competencia/lancamentos', sindico, ctrl.lancar);
router.delete('/admin/prestacao/lancamentos/:id', sindico, ctrl.excluirLancamento);
router.post('/admin/prestacao/:competencia/publicar', sindico, ctrl.publicar);

router.get('/prestacao', morador, ctrl.publicadas);
router.get('/prestacao/:competencia', morador, ctrl.publicada);

export default router;
