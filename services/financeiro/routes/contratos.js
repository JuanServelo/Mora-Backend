import express from 'express';
import { autenticar, exigirPerfis, PERFIS_GESTAO, PERFIS_UNIDADE } from '../middleware/auth.js';
import { resolverEscopo, resolverUnidade, exigirEscrita } from '../middleware/escopo.js';
import * as controller from '../controllers/contratosController.js';
import { embrulhar } from '../utils/assincrono.js';

const ctrl = embrulhar(controller);
const router = express.Router();

const gestao = [autenticar, exigirPerfis(...PERFIS_GESTAO), resolverEscopo];
const sindico = [autenticar, exigirPerfis('ADMIN_SINDICO'), resolverEscopo, exigirEscrita];
// Morador e dono veem o contrato da unidade — o inquilino também precisa saber
// o que foi combinado. Só o dono cadastra e encerra.
const unidade = [autenticar, exigirPerfis(...PERFIS_UNIDADE), resolverEscopo, resolverUnidade];
const dono = [autenticar, exigirPerfis('DONO_ALUGUEL'), resolverEscopo, resolverUnidade];

router.get('/admin/contratos', gestao, ctrl.listar);
router.post('/admin/contratos', sindico, ctrl.criar);
router.patch('/admin/contratos/:id/encerrar', sindico, ctrl.encerrar);

router.get('/contratos/meus', unidade, ctrl.daUnidade);
router.post('/contratos/meus', dono, ctrl.criarComoDono);
router.patch('/contratos/:id/encerrar', dono, ctrl.encerrarComoDono);

export default router;
