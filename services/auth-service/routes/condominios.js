import express from 'express';
import authMiddleware, { adminMiddleware } from '../middleware/auth.js';
import { PERFIS } from '../constants/perfis.js';
import {
  listCondominios,
  getCondominio,
  createCondominio,
  updateCondominio,
  deactivateCondominio,
  activateCondominio,
  listUsersInCondominio,
  assignUserToCondominio,
} from '../controllers/condominiosController.js';

const router = express.Router();

function gerenteMiddleware(req, res, next) {
  const { userPerfil } = req;
  const permitidos = [PERFIS.CONTRACTING_PROPERTY_MANAGER, PERFIS.CONTRACTING_SYNDIC];
  if (!permitidos.includes(userPerfil)) {
    return res.status(403).json({ sucesso: false, mensagem: 'Apenas administradoras ou síndicos contratantes podem gerenciar clientes.' });
  }
  next();
}

router.use(authMiddleware);

router.get('/', listCondominios);
router.get('/:id', getCondominio);
router.post('/', adminMiddleware, gerenteMiddleware, createCondominio);
router.put('/:id', adminMiddleware, updateCondominio);
router.patch('/:id/deactivate', adminMiddleware, gerenteMiddleware, deactivateCondominio);
router.patch('/:id/activate', adminMiddleware, gerenteMiddleware, activateCondominio);
router.get('/:id/users', adminMiddleware, listUsersInCondominio);
router.patch('/:id/assign-user', adminMiddleware, assignUserToCondominio);

export default router;
