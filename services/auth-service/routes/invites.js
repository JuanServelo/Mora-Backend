import express from 'express';
import rateLimit from 'express-rate-limit';
import { validateInvite, activateAccount } from '../controllers/invitesController.js';

const router = express.Router();

const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 10,
  message: { sucesso: false, mensagem: 'Muitas tentativas. Tente novamente em 15 minutos.' },
  standardHeaders: true,
  legacyHeaders: false,
});

router.post('/validate', authLimiter, validateInvite);
router.post('/activate', authLimiter, activateAccount);

export default router;
