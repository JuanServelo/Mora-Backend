import express from 'express';
import authMiddleware, { adminMiddleware } from '../middleware/auth.js';
import { listAllPerfisInfo, getPerfilInfo } from '../controllers/perfisController.js';

const router = express.Router();

router.use(authMiddleware, adminMiddleware);

router.get('/info', listAllPerfisInfo);
router.get('/info/:perfil', getPerfilInfo);

export default router;
