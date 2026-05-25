import { Op } from 'sequelize';
import User from '../models/User.js';
import Invite from '../models/Invite.js';
import {
  PERFIS,
  STATUS_USUARIO,
  STATUS_CONVITE,
  podeCadastrarPerfil,
} from '../constants/perfis.js';
import { possuiCobrancasEmAberto } from '../utils/financialGuardService.js';
import {
  criarConvite,
  reenviarConvite,
  validarDadosConviteAdmin,
  residentOwnerAtivoNaUnidade,
  lesseeAtivoNaUnidade,
} from './inviteService.js';

export async function listarUsuariosEscopo(ator) {
  const perfil = ator.getPerfilEfetivo();
  const condominioId = ator.condominioId;

  let whereUsers = { condominioId };
  let whereInvites = {
    condominioId,
    status: STATUS_CONVITE.PENDING,
    expiresAt: { [Op.gt]: new Date() },
  };

  if ([PERFIS.RESIDENT_OWNER, PERFIS.ABSENT_OWNER, PERFIS.LESSEE].includes(perfil)) {
    whereUsers = { ...whereUsers, unidadeId: ator.unidadeId };
    whereInvites = { ...whereInvites, unidadeId: ator.unidadeId };
  }

  const [usuarios, convitesPendentes] = await Promise.all([
    User.findAll({ where: whereUsers, order: [['createdAt', 'DESC']] }),
    Invite.findAll({ where: whereInvites, order: [['createdAt', 'DESC']] }),
  ]);

  return { usuarios, convitesPendentes };
}

export async function emitirConvite(ator, dados) {
  const perfilAtor = ator.getPerfilEfetivo();
  const { email, perfil, unidadeId, nomePrecadastro, cpfPrecadastro } = dados;

  if (!podeCadastrarPerfil(perfilAtor, perfil)) {
    return {
      sucesso: false,
      mensagem: 'Você não tem permissão para cadastrar este perfil.',
      status: 403,
    };
  }

  const unidadeEfetiva = unidadeId || ator.unidadeId || null;

  if (perfilAtor === PERFIS.ABSENT_OWNER && perfil === PERFIS.LESSEE) {
    const lesseeExistente = await lesseeAtivoNaUnidade(unidadeEfetiva);
    if (lesseeExistente) {
      return {
        sucesso: false,
        mensagem: 'Você já possui um Lessee vinculado a esta unidade.',
        status: 400,
      };
    }
  }

  if (perfil === PERFIS.RESIDENT_OWNER && unidadeEfetiva) {
    const roAtivo = await residentOwnerAtivoNaUnidade(unidadeEfetiva);
    if (roAtivo) {
      return {
        sucesso: false,
        mensagem: 'Esta unidade já possui um Resident Owner ativo. Desative o atual antes de cadastrar um novo.',
        status: 400,
      };
    }
  }

  const validacao = await validarDadosConviteAdmin({
    email,
    perfil,
    unidadeId: unidadeEfetiva,
    nomePrecadastro,
    cpfPrecadastro,
    condominioId: ator.condominioId,
  });

  if (!validacao.sucesso) {
    return { ...validacao, status: 400 };
  }

  const { invite: convite, emailEnviado, avisoEmail } = await criarConvite({
    email,
    perfil,
    cadastradoPorId: ator.id,
    condominioId: ator.condominioId,
    unidadeId: unidadeEfetiva,
    nomePrecadastro,
    cpfPrecadastro,
  });

  return {
    sucesso: true,
    mensagem: emailEnviado
      ? 'Convite enviado com sucesso.'
      : 'Convite criado, mas o e-mail não pôde ser enviado.',
    emailEnviado,
    avisoEmail: avisoEmail || null,
    convite: {
      id: convite.id,
      email: convite.email,
      perfil: convite.perfil,
      codigo: convite.codigo,
      expiresAt: convite.expiresAt,
    },
  };
}

async function desativarUsuarioRecursivo(usuario) {
  usuario.status = STATUS_USUARIO.INACTIVE;
  usuario.tokenVersion = (usuario.tokenVersion || 0) + 1;
  await usuario.save();
}

export async function desativarUsuario(ator, alvoId) {
  const alvo = await User.findByPk(alvoId);
  if (!alvo) {
    return { sucesso: false, mensagem: 'Usuário não encontrado', status: 404 };
  }

  if (alvo.status === STATUS_USUARIO.INACTIVE) {
    return { sucesso: false, mensagem: 'Usuário já está inativo.', status: 400 };
  }

  const perfilAtor = ator.getPerfilEfetivo();
  const perfilAlvo = alvo.getPerfilEfetivo();

  if (!podeDesativar(perfilAtor, perfilAlvo, ator, alvo)) {
    return {
      sucesso: false,
      mensagem: 'Você não tem permissão para desativar este usuário.',
      status: 403,
    };
  }

  if (alvo.responsavelFinanceiro && alvo.unidadeId) {
    const temCobrancas = await possuiCobrancasEmAberto(alvo.id, alvo.unidadeId);
    if (temCobrancas) {
      return {
        sucesso: false,
        mensagem: `Este usuário é o responsável financeiro da unidade e possui cobranças em aberto. Regularize as pendências antes de prosseguir.`,
        status: 400,
      };
    }
  }

  const cascata = await buscarUsuariosCascata(alvo);
  const totalCascata = cascata.length;

  await desativarUsuarioRecursivo(alvo);
  for (const u of cascata) {
    await desativarUsuarioRecursivo(u);
  }

  let mensagem = 'Usuário desativado com sucesso.';
  if (perfilAlvo === PERFIS.LESSEE && totalCascata > 0) {
    mensagem = `Lessee desativado. ${totalCascata} usuários vinculados também foram desativados.`;
  } else if (totalCascata > 0) {
    mensagem = `Usuário desativado. ${totalCascata} usuários vinculados também foram desativados.`;
  }

  return {
    sucesso: true,
    mensagem,
    desativados: totalCascata + 1,
  };
}

function podeDesativar(perfilAtor, perfilAlvo, ator, alvo) {
  const escopoCondominio = [
    PERFIS.CONTRACTING_PROPERTY_MANAGER,
    PERFIS.CONTRACTING_SYNDIC,
  ];

  if (escopoCondominio.includes(perfilAtor)) {
    return ator.condominioId === alvo.condominioId;
  }

  if (perfilAtor === PERFIS.RESIDENT_OWNER || perfilAtor === PERFIS.ABSENT_OWNER) {
    if (alvo.cadastradoPorId !== ator.id && alvo.unidadeId !== ator.unidadeId) {
      return false;
    }
    return [PERFIS.LESSEE, PERFIS.OCCUPANT, PERFIS.GUEST].includes(perfilAlvo)
      || (perfilAlvo === PERFIS.RESIDENT_OWNER && ator.unidadeId === alvo.unidadeId);
  }

  if (perfilAtor === PERFIS.LESSEE) {
    return alvo.cadastradoPorId === ator.id
      && [PERFIS.OCCUPANT, PERFIS.GUEST].includes(perfilAlvo);
  }

  return false;
}

async function buscarUsuariosCascata(alvo) {
  const perfil = alvo.getPerfilEfetivo();
  const desativados = [];

  if (perfil === PERFIS.LESSEE) {
    const vinculados = await User.findAll({
      where: {
        cadastradoPorId: alvo.id,
        status: STATUS_USUARIO.ACTIVE,
        perfil: { [Op.in]: [PERFIS.OCCUPANT, PERFIS.GUEST] },
      },
    });
    desativados.push(...vinculados);
  }

  if (perfil === PERFIS.RESIDENT_OWNER && alvo.responsavelFinanceiro) {
    const vinculados = await User.findAll({
      where: {
        unidadeId: alvo.unidadeId,
        status: STATUS_USUARIO.ACTIVE,
        id: { [Op.ne]: alvo.id },
      },
    });
    desativados.push(...vinculados);
  }

  return desativados;
}

export { reenviarConvite };
