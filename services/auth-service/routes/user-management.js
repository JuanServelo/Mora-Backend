import express from 'express';
import authMiddleware, { adminMiddleware, gestaoMiddleware } from '../middleware/auth.js';
import { PERFIS, podeGerenciarOcupantes, podeGerenciarUsuarios } from '../constants/perfis.js';
import {
  listUsersAndInvites,
  createInvite,
  resendInvite,
  deactivateUser,
  getUnitOccupants,
  checkTransferEligibility,
  createLessee,
  createOccupant,
  createGuest,
  transferResponsibility,
  removeOccupant,
  assignUserToUnit,
  removeUserFromUnit,
  listUnitResidents,
} from '../controllers/userManagementController.js';

const router = express.Router();

function occupantUnitMiddleware(req, res, next) {
  const { unidadeId } = req.params;
  const perfil = req.userPerfil;

  if (podeGerenciarOcupantes(perfil)) {
    if (req.user.unidadeId !== unidadeId) {
      return res.status(403).json({
        sucesso: false,
        mensagem: 'Você não tem permissão para acessar esta unidade.',
      });
    }
  } else if (!podeGerenciarUsuarios(perfil)) {
    return res.status(403).json({
      sucesso: false,
      mensagem: 'Você não tem permissão para acessar esta funcionalidade.',
    });
  }

  next();
}

function residentsMiddleware(req, res, next) {
  const { unidadeId } = req.params;
  const perfil = req.userPerfil;
  const isPM = [PERFIS.CONTRACTING_PROPERTY_MANAGER, PERFIS.CONTRACTING_SYNDIC, PERFIS.OPERATIONAL_SYNDIC, PERFIS.ADMINISTRATOR].includes(perfil);
  if (!isPM && req.user.unidadeId !== unidadeId) {
    return res.status(403).json({ sucesso: false, mensagem: 'Acesso negado.' });
  }
  next();
}

router.use(authMiddleware, gestaoMiddleware);

router.get('/users', listUsersAndInvites);
router.post('/invites', createInvite);
router.post('/invites/:id/resend', resendInvite);
router.patch('/users/:id/deactivate', deactivateUser);

router.get('/units/:unidadeId/occupants', occupantUnitMiddleware, getUnitOccupants);
router.get('/units/:unidadeId/transfer-eligibility', occupantUnitMiddleware, checkTransferEligibility);
router.post('/units/:unidadeId/occupants/lessee', occupantUnitMiddleware, createLessee);
router.post('/units/:unidadeId/occupants/occupant', occupantUnitMiddleware, createOccupant);
router.post('/units/:unidadeId/occupants/guest', occupantUnitMiddleware, createGuest);
router.post('/units/:unidadeId/transfer-financial-responsibility', occupantUnitMiddleware, transferResponsibility);
router.delete('/units/:unidadeId/occupants/:userId', occupantUnitMiddleware, removeOccupant);

router.patch('/users/:id/unit', adminMiddleware, assignUserToUnit);
router.delete('/users/:id/unit', adminMiddleware, removeUserFromUnit);
router.get('/units/:unidadeId/residents', residentsMiddleware, listUnitResidents);

export default router;
