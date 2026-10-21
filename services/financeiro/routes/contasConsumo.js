import express from 'express';
import { autenticar, exigirPerfis, PERFIS_GESTAO } from '../middleware/auth.js';
import { resolverEscopo, exigirEscrita } from '../middleware/escopo.js';
import * as ctrl from '../controllers/contasConsumoController.js';
import { protegerRotas } from '../utils/assincrono.js';

const router = express.Router();

const adm = [autenticar, exigirPerfis(...PERFIS_GESTAO), resolverEscopo];
const admW = [...adm, exigirEscrita];

router.get('/contas-consumo', adm, ctrl.listar);
router.post('/contas-consumo', admW, ctrl.criar);
router.put('/contas-consumo/:id', admW, ctrl.atualizar);
router.delete('/contas-consumo/:id', admW, ctrl.cancelar);
router.post('/contas-consumo/:id/ratear', admW, ctrl.ratear);

// Erro num handler async vira 500 com mensagem genérica, em vez de deixar a
// requisição sem resposta e derrubar o processo (Express 4).
export default protegerRotas(router);
