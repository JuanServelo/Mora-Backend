import express from 'express';
import { Op } from 'sequelize';
import sequelize from '../config/database.js';
import authMiddleware from '../middleware/auth.js';
import User from '../models/User.js';
import RegistroAcesso from '../models/RegistroAcesso.js';
import { PERFIS, STATUS_USUARIO, podeAcessarAdmin } from '../constants/perfis.js';
import { usuarioPublico } from '../utils/usuarioPublico.js';

const router = express.Router();

router.use(authMiddleware);

function portariaMiddleware(req, res, next) {
  const p = req.userPerfil;
  if (p === PERFIS.PORTEIRO || podeAcessarAdmin(p)) return next();
  return res.status(403).json({ sucesso: false, mensagem: 'Apenas porteiros têm acesso a esta funcionalidade.' });
}

function responsavelMiddleware(req, res, next) {
  const p = req.userPerfil;
  const permitidos = [
    PERFIS.MORADOR, PERFIS.DONO_ALUGUEL,
    PERFIS.ADMIN_GERAL, PERFIS.ADMIN_SINDICO,
  ];
  if (permitidos.includes(p)) return next();
  return res.status(403).json({ sucesso: false, mensagem: 'Sem permissão para gerenciar convidados.' });
}

async function ultimoRegistroPorUsuario(usuarioIds, condominioId) {
  if (!usuarioIds.length) return {};
  const rows = await sequelize.query(`
    SELECT DISTINCT ON ("usuarioId") "usuarioId", tipo, "createdAt"
    FROM registros_acesso
    WHERE "usuarioId" IN (:ids) AND "condominioId" = :condominioId
    ORDER BY "usuarioId", "createdAt" DESC
  `, {
    replacements: { ids: usuarioIds, condominioId },
    type: sequelize.QueryTypes.SELECT,
  });
  return Object.fromEntries(rows.map((r) => [r.usuarioId, r]));
}

function mapUsuario(u, ultimoRegistro) {
  const pub = usuarioPublico(u);
  const ultimo = ultimoRegistro?.[u.id];
  return {
    ...pub,
    statusAcesso: ultimo?.tipo === 'ENTRADA' ? 'DENTRO' : 'FORA',
    ultimoRegistroEm: ultimo?.createdAt ?? null,
  };
}

// ─── PORTEIRO: listar convidados (GUEST) do condomínio ───
router.get('/guests', portariaMiddleware, async (req, res) => {
  try {
    const condominioId = req.user.condominioId;
    const guests = await User.findAll({
      where: { condominioId, perfil: PERFIS.CONVIDADO, status: STATUS_USUARIO.ACTIVE },
      order: [['nome', 'ASC']],
    });
    const ids = guests.map((g) => g.id);
    const ultimos = await ultimoRegistroPorUsuario(ids, condominioId);
    res.json({ sucesso: true, guests: guests.map((g) => mapUsuario(g, ultimos)) });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// ─── PORTEIRO: listar residentes (todos exceto GUEST) do condomínio ───
router.get('/residentes', portariaMiddleware, async (req, res) => {
  try {
    const condominioId = req.user.condominioId;
    const residentes = await User.findAll({
      where: {
        condominioId,
        status: STATUS_USUARIO.ACTIVE,
        perfil: { [Op.ne]: PERFIS.CONVIDADO },
        semAcessoSistema: false,
      },
      order: [['nome', 'ASC']],
    });
    const ids = residentes.map((u) => u.id);
    const ultimos = await ultimoRegistroPorUsuario(ids, condominioId);
    res.json({ sucesso: true, residentes: residentes.map((u) => mapUsuario(u, ultimos)) });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// ─── PORTEIRO: quem está dentro agora ───
router.get('/dentro', portariaMiddleware, async (req, res) => {
  try {
    const condominioId = req.user.condominioId;
    const rows = await sequelize.query(`
      SELECT DISTINCT ON ("usuarioId") "usuarioId", tipo, "createdAt"
      FROM registros_acesso
      WHERE "condominioId" = :condominioId
      ORDER BY "usuarioId", "createdAt" DESC
    `, {
      replacements: { condominioId },
      type: sequelize.QueryTypes.SELECT,
    });
    const dentroIds = rows.filter((r) => r.tipo === 'ENTRADA').map((r) => r.usuarioId);
    if (!dentroIds.length) return res.json({ sucesso: true, dentro: [] });

    const usuarios = await User.findAll({
      where: { id: { [Op.in]: dentroIds }, status: STATUS_USUARIO.ACTIVE },
      order: [['nome', 'ASC']],
    });
    const ultimoMap = Object.fromEntries(rows.map((r) => [r.usuarioId, r]));
    res.json({ sucesso: true, dentro: usuarios.map((u) => mapUsuario(u, ultimoMap)) });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// ─── PORTEIRO: registrar entrada ───
router.post('/entrada/:userId', portariaMiddleware, async (req, res) => {
  try {
    const alvo = await User.findByPk(Number(req.params.userId));
    if (!alvo || alvo.status !== STATUS_USUARIO.ACTIVE) {
      return res.status(404).json({ sucesso: false, mensagem: 'Usuário não encontrado ou inativo.' });
    }

    if (alvo.condominioId !== req.user.condominioId) {
      return res.status(403).json({ sucesso: false, mensagem: 'Usuário não pertence a este condomínio.' });
    }

    if (alvo.perfil === PERFIS.CONVIDADO && !alvo.entradaPermitida) {
      return res.status(403).json({
        sucesso: false,
        mensagem: 'Entrada não permitida. O responsável da unidade não autorizou a entrada deste convidado.',
      });
    }

    await RegistroAcesso.create({
      usuarioId: alvo.id,
      tipo: 'ENTRADA',
      registradoPorId: req.user.id,
      condominioId: req.user.condominioId,
    });

    res.json({ sucesso: true, mensagem: `Entrada de ${alvo.nome} registrada.` });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// ─── PORTEIRO: registrar saída ───
router.post('/saida/:userId', portariaMiddleware, async (req, res) => {
  try {
    const alvo = await User.findByPk(Number(req.params.userId));
    if (!alvo || alvo.status !== STATUS_USUARIO.ACTIVE) {
      return res.status(404).json({ sucesso: false, mensagem: 'Usuário não encontrado ou inativo.' });
    }

    if (alvo.condominioId !== req.user.condominioId) {
      return res.status(403).json({ sucesso: false, mensagem: 'Usuário não pertence a este condomínio.' });
    }

    await RegistroAcesso.create({
      usuarioId: alvo.id,
      tipo: 'SAIDA',
      registradoPorId: req.user.id,
      condominioId: req.user.condominioId,
    });

    res.json({ sucesso: true, mensagem: `Saída de ${alvo.nome} registrada.` });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// ─── RESPONSÁVEL: ver meus convidados ───
router.get('/meus-guests', responsavelMiddleware, async (req, res) => {
  try {
    const { unidadeId, condominioId, id: userId } = req.user;

    const where = {
      perfil: PERFIS.CONVIDADO,
      status: STATUS_USUARIO.ACTIVE,
      condominioId,
    };

    if (unidadeId) {
      where.unidadeId = unidadeId;
    } else {
      where.cadastradoPorId = userId;
    }

    const guests = await User.findAll({ where, order: [['nome', 'ASC']] });
    const ids = guests.map((g) => g.id);
    const ultimos = await ultimoRegistroPorUsuario(ids, condominioId);

    res.json({ sucesso: true, guests: guests.map((g) => mapUsuario(g, ultimos)) });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// ─── RESPONSÁVEL: alternar permissão de entrada do convidado ───
router.patch('/guests/:guestId/permissao', responsavelMiddleware, async (req, res) => {
  try {
    const guest = await User.findByPk(Number(req.params.guestId));
    if (!guest || guest.perfil !== PERFIS.CONVIDADO || guest.status !== STATUS_USUARIO.ACTIVE) {
      return res.status(404).json({ sucesso: false, mensagem: 'Convidado não encontrado.' });
    }

    if (guest.condominioId !== req.user.condominioId) {
      return res.status(403).json({ sucesso: false, mensagem: 'Convidado não pertence a este condomínio.' });
    }

    const p = req.userPerfil;
    const isAdmin = podeAcessarAdmin(p);

    if (!isAdmin) {
      const responsavel = req.user.responsavelFinanceiro && req.user.unidadeId === guest.unidadeId;
      const criador = guest.cadastradoPorId === req.user.id;
      if (!responsavel && !criador) {
        return res.status(403).json({
          sucesso: false,
          mensagem: 'Apenas o responsável da unidade pode autorizar a entrada de convidados.',
        });
      }
    }

    const { permitir } = req.body;
    guest.entradaPermitida = typeof permitir === 'boolean' ? permitir : !guest.entradaPermitida;
    await guest.save();

    const acao = guest.entradaPermitida ? 'autorizada' : 'bloqueada';
    res.json({
      sucesso: true,
      mensagem: `Entrada de ${guest.nome} ${acao}.`,
      entradaPermitida: guest.entradaPermitida,
    });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// ─── PORTEIRO: listar todos os usuários do condomínio ───
router.get('/usuarios-condominio', portariaMiddleware, async (req, res) => {
  try {
    const condominioId = req.user.condominioId;
    const usuarios = await User.findAll({
      where: { condominioId, status: STATUS_USUARIO.ACTIVE },
      order: [['nome', 'ASC']],
    });
    res.json({ sucesso: true, usuarios: usuarios.map(usuarioPublico) });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

// ─── PORTEIRO: histórico de acessos de um usuário ───
router.get('/historico/:userId', portariaMiddleware, async (req, res) => {
  try {
    const registros = await RegistroAcesso.findAll({
      where: {
        usuarioId: Number(req.params.userId),
        condominioId: req.user.condominioId,
      },
      order: [['createdAt', 'DESC']],
      limit: 50,
    });
    res.json({ sucesso: true, registros });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
});

export default router;
