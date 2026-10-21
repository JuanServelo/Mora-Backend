import express from 'express';
import * as ctrl from '../controllers/webhookController.js';
import { protegerRotas } from '../utils/assincrono.js';

const router = express.Router();

// O parse do corpo cru já acontece no server.js antes do JSON global.
router.post('/webhooks', ctrl.processar);

// Erro num handler async vira 500 com mensagem genérica, em vez de deixar a
// requisição sem resposta e derrubar o processo (Express 4).
export default protegerRotas(router);
