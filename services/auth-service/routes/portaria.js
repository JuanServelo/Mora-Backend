import express from 'express';
import authMiddleware from '../middleware/auth.js';
import { PERFIS, podeAcessarAdmin } from '../constants/perfis.js';
import {
  listGuests,
  listResidentes,
  listDentro,
  registrarEntrada,
  registrarSaida,
  getMeusGuests,
  patchGuestPermissao,
  listUsuariosCondominio,
  getHistoricoAcesso,
} from '../controllers/portariaController.js';

const router = express.Router();

router.use(authMiddleware);

function portariaMiddleware(req, res, next) {
  const p = req.userPerfil;
  if (p === PERFIS.DOORMAN || podeAcessarAdmin(p)) return next();
  return res.status(403).json({ sucesso: false, mensagem: 'Apenas porteiros têm acesso a esta funcionalidade.' });
}

function responsavelMiddleware(req, res, next) {
  const p = req.userPerfil;
  const permitidos = [
    PERFIS.RESIDENT_OWNER, PERFIS.LESSEE,
    PERFIS.CONTRACTING_PROPERTY_MANAGER, PERFIS.CONTRACTING_SYNDIC,
    PERFIS.OPERATIONAL_SYNDIC, PERFIS.ADMINISTRATOR,
  ];
  if (permitidos.includes(p)) return next();
  return res.status(403).json({ sucesso: false, mensagem: 'Sem permissão para gerenciar convidados.' });
}

router.get('/guests', portariaMiddleware, listGuests);
router.get('/residentes', portariaMiddleware, listResidentes);
router.get('/dentro', portariaMiddleware, listDentro);
router.post('/entrada/:userId', portariaMiddleware, registrarEntrada);
router.post('/saida/:userId', portariaMiddleware, registrarSaida);
router.get('/meus-guests', responsavelMiddleware, getMeusGuests);
router.patch('/guests/:guestId/permissao', responsavelMiddleware, patchGuestPermissao);
router.get('/usuarios-condominio', portariaMiddleware, listUsuariosCondominio);
router.get('/historico/:userId', portariaMiddleware, getHistoricoAcesso);

export default router;
