import { PERFIS_EXIGEM_UNIDADE } from '../constants/perfis.js';
import {
  listarUsuariosEscopo,
  emitirConvite,
  reenviarConvite,
  desativarUsuario,
} from '../services/userManagementService.js';
import {
  listarOcupantesUnidade,
  cadastrarLessee,
  cadastrarOccupant,
  cadastrarGuest,
  transferirResponsabilidadeFinanceira,
  verificarElegibilidadeTransferencia,
  removerVinculo,
} from '../services/occupantService.js';
import { validarUnidadeExiste } from '../utils/portariaClient.js';
import User from '../models/User.js';
import { usuarioPublico } from '../utils/usuarioPublico.js';

export const listUsersAndInvites = async (req, res) => {
  try {
    const { usuarios, convitesPendentes } = await listarUsuariosEscopo(req.user);

    res.json({
      sucesso: true,
      usuarios: usuarios.map(usuarioPublico),
      convitesPendentes: convitesPendentes.map((c) => ({
        id: c.id,
        email: c.email,
        perfil: c.perfil,
        unidadeId: c.unidadeId,
        status: c.status,
        expiresAt: c.expiresAt,
        createdAt: c.createdAt,
        expirado: new Date(c.expiresAt) < new Date(),
      })),
    });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
};

export const createInvite = async (req, res) => {
  try {
    const { email, perfil, unidadeId, nomePrecadastro, cpfPrecadastro, condominioId } = req.body;

    if (!email || !perfil) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'E-mail e perfil são obrigatórios.',
      });
    }

    const condominioEfetivo = req.user.role === 'admin'
      ? (condominioId || req.user.condominioId || null)
      : (req.user.condominioId || condominioId || null);

    if (!condominioEfetivo) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'Selecione o condomínio para o qual o convite será emitido.',
      });
    }

    const resultado = await emitirConvite(req.user, {
      email,
      perfil,
      unidadeId,
      nomePrecadastro,
      cpfPrecadastro,
      condominioId: condominioEfetivo,
    });

    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }

    res.status(201).json(resultado);
  } catch (err) {
    console.error('Erro emitir convite:', err);
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
};

export const resendInvite = async (req, res) => {
  try {
    const resultado = await reenviarConvite(Number(req.params.id), req.user.id);

    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }

    res.json({
      sucesso: true,
      mensagem: resultado.emailEnviado
        ? 'Convite reenviado com sucesso.'
        : 'Convite reenviado, mas o e-mail não pôde ser enviado.',
      emailEnviado: resultado.emailEnviado,
      avisoEmail: resultado.avisoEmail || null,
      convite: {
        id: resultado.convite.id,
        email: resultado.convite.email,
        codigo: resultado.convite.codigo,
        expiresAt: resultado.convite.expiresAt,
      },
    });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
};

export const deactivateUser = async (req, res) => {
  try {
    const resultado = await desativarUsuario(req.user, Number(req.params.id));

    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }

    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
};

export const getUnitOccupants = async (req, res) => {
  try {
    const resultado = await listarOcupantesUnidade(req.user, req.params.unidadeId);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
};

export const checkTransferEligibility = async (req, res) => {
  try {
    const resultado = await verificarElegibilidadeTransferencia(req.user, req.params.unidadeId);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
};

export const createLessee = async (req, res) => {
  try {
    const resultado = await cadastrarLessee(req.user, req.params.unidadeId, req.body);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.status(201).json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
};

export const createOccupant = async (req, res) => {
  try {
    const resultado = await cadastrarOccupant(req.user, req.params.unidadeId, req.body);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.status(201).json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
};

export const createGuest = async (req, res) => {
  try {
    const resultado = await cadastrarGuest(req.user, req.params.unidadeId, req.body);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.status(201).json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
};

export const transferResponsibility = async (req, res) => {
  try {
    const resultado = await transferirResponsabilidadeFinanceira(req.user, req.params.unidadeId);
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
};

export const removeOccupant = async (req, res) => {
  try {
    const resultado = await removerVinculo(req.user, req.params.unidadeId, Number(req.params.userId));
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
};

export const assignUserToUnit = async (req, res) => {
  try {
    const { unidadeId } = req.body;

    const alvo = await User.findByPk(Number(req.params.id));
    if (!alvo) return res.status(404).json({ sucesso: false, mensagem: 'Usuário não encontrado.' });

    if (!PERFIS_EXIGEM_UNIDADE.includes(alvo.getPerfilEfetivo())) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'Este perfil não pode ser vinculado a uma unidade.',
      });
    }

    if (!unidadeId) return res.status(400).json({ sucesso: false, mensagem: 'unidadeId é obrigatório.' });

    const unidadeValida = await validarUnidadeExiste(unidadeId);
    if (!unidadeValida) return res.status(404).json({ sucesso: false, mensagem: 'Unidade não encontrada.' });

    alvo.unidadeId = unidadeId;
    await alvo.save();

    res.json({ sucesso: true, mensagem: 'Usuário vinculado à unidade.', usuario: usuarioPublico(alvo) });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
};

export const removeUserFromUnit = async (req, res) => {
  try {
    const alvo = await User.findByPk(Number(req.params.id));
    if (!alvo) return res.status(404).json({ sucesso: false, mensagem: 'Usuário não encontrado.' });

    if (alvo.responsavelFinanceiro) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'Não é possível desvincular o responsável financeiro da unidade.',
      });
    }

    alvo.unidadeId = null;
    await alvo.save();

    res.json({ sucesso: true, mensagem: 'Vínculo com unidade removido.', usuario: usuarioPublico(alvo) });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
};

export const listUnitResidents = async (req, res) => {
  try {
    const { unidadeId } = req.params;

    const moradores = await User.findAll({
      where: { unidadeId, status: 'active' },
      order: [['nome', 'ASC']],
    });

    res.json({ sucesso: true, moradores: moradores.map(usuarioPublico) });
  } catch (err) {
    res.status(500).json({ sucesso: false, mensagem: err.message });
  }
};
