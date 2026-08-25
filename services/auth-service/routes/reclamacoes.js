import express from 'express';
import authMiddleware, { adminMiddleware } from '../middleware/auth.js';
import { create, listMine, listAll, update } from '../controllers/reclamacoesController.js';

const router = express.Router();

router.post('/', authMiddleware, create);
router.get('/minhas', authMiddleware, listMine);
router.get('/todas', authMiddleware, adminMiddleware, listAll);
router.patch('/:id', authMiddleware, adminMiddleware, update);

export default router;
