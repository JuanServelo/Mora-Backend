import express from 'express';
import authMiddleware, { adminMiddleware, gestaoMiddleware } from '../middleware/auth.js';
import { listUsers, createUser, updateUser, deleteUser } from '../controllers/usersController.js';

const router = express.Router();

router.get('/', authMiddleware, listUsers);
router.post('/', authMiddleware, gestaoMiddleware, createUser);
router.put('/:id', authMiddleware, adminMiddleware, updateUser);
router.delete('/:id', authMiddleware, gestaoMiddleware, deleteUser);

export default router;
