import { Op } from 'sequelize';
import User from '../models/User.js';
import Invite from '../models/Invite.js';
import {
  PERFIS,
  STATUS_USUARIO,
  STATUS_CONVITE,
  podeCadastrarPerfil,
} from '../constants/perfis.js';
import { MSG_RF07 } from '../constants/occupantMessages.js';
import {
  criarConvite,
  reenviarConvite,
  validarDadosConviteAdmin,
  lesseeAtivoNaUnidade,
} from './inviteService.js';

export async function listarUsuariosEscopo(ator, filtros = {}) {
  const perfil = ator.getPerfilEfetivo();

  /**
   * O Admin Geral opera a plataforma e tem `condominioId` nulo. Sem este ramo,
   * a consulta virava `WHERE "condominioId" IS NULL` e ele não via usuário
   * algum. Ele enxerga tudo, ou um cliente específico via ?condominioId=.
   */
  const ehAdminGeral = perfil === PERFIS.ADMIN_GERAL;
  const escopo = ehAdminGeral
    ? (filtros.condominioId ? { condominioId: filtros.condominioId } : {})
    : { condominioId: ator.condominioId };

  let whereUsers = { ...escopo };
  let whereInvites = {
    ...escopo,
    status: STATUS_CONVITE.PENDING,
    expiresAt: { [Op.gt]: new Date() },
  };

  if ([PERFIS.MORADOR, PERFIS.DONO_ALUGUEL].includes(perfil)) {
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
  const { email, perfil, unidadeId, nomePrecadastro, cpfPrecadastro, condominioId: condominioIdDados } = dados;
  const condominioId = condominioIdDados || ator.condominioId;

  if (!podeCadastrarPerfil(perfilAtor, perfil)) {
    return {
      sucesso: false,
      mensagem: 'Você não tem permissão para cadastrar este perfil.',
      status: 403,
    };
  }

  if (perfil === PERFIS.CONVIDADO) {
    return {
      sucesso: false,
      mensagem: MSG_RF07.GUEST_SEM_ACESSO,
      status: 400,
    };
  }

  const unidadeEfetiva = unidadeId || ator.unidadeId || null;

  // Só um morador responsável financeiro por unidade.
  if (perfil === PERFIS.MORADOR && perfilAtor === PERFIS.DONO_ALUGUEL) {
    const lesseeExistente = await lesseeAtivoNaUnidade(unidadeEfetiva);
    if (lesseeExistente) {
      return {
        sucesso: false,
        mensagem: MSG_RF07.LESSEE_DUPLICADO,
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
    condominioId,
  });

  if (!validacao.sucesso) {
    return { ...validacao, status: 400 };
  }

  const { invite: convite, emailEnviado, avisoEmail } = await criarConvite({
    email,
    perfil,
    cadastradoPorId: ator.id,
    condominioId,
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

export async function desativarUsuario(ator, alvoId, opts = {}) {
  const alvo = await User.findByPk(alvoId);
  if (!alvo) {
    return {
      sucesso: false,
      mensagem: opts.mensagemRemocao ? MSG_RF07.VINCULO_INEXISTENTE : 'Usuário não encontrado',
      status: 404,
    };
  }

  if (alvo.status === STATUS_USUARIO.INACTIVE) {
    return {
      sucesso: false,
      mensagem: opts.mensagemRemocao ? MSG_RF07.VINCULO_INEXISTENTE : 'Usuário já está inativo.',
      status: 400,
    };
  }

  const perfilAtor = ator.getPerfilEfetivo();
  const perfilAlvo = alvo.getPerfilEfetivo();

  if (!podeDesativar(perfilAtor, perfilAlvo, ator, alvo)) {
    return {
      sucesso: false,
      mensagem: opts.mensagemRemocao
        ? MSG_RF07.SEM_PERMISSAO_REMOVER
        : 'Você não tem permissão para desativar este usuário.',
      status: 403,
    };
  }

  if (alvo.responsavelFinanceiro && alvo.unidadeId) {
    return {
      sucesso: false,
      mensagem: MSG_RF07.REMOVER_FINANCEIRO,
      status: 400,
    };
  }

  const cascata = await buscarUsuariosCascata(alvo);
  const totalCascata = cascata.length;

  await desativarUsuarioRecursivo(alvo);
  for (const u of cascata) {
    await desativarUsuarioRecursivo(u);
  }

  let mensagem = opts.mensagemRemocao ? MSG_RF07.VINCULO_REMOVIDO : 'Usuário desativado com sucesso.';
  if (perfilAlvo === PERFIS.MORADOR && totalCascata > 0) {
    mensagem = opts.mensagemRemocao
      ? `Morador removido. ${totalCascata} usuários vinculados também foram desativados.`
      : `Morador desativado. ${totalCascata} usuários vinculados também foram desativados.`;
  } else if (totalCascata > 0 && !opts.mensagemRemocao) {
    mensagem = `Usuário desativado. ${totalCascata} usuários vinculados também foram desativados.`;
  }

  return {
    sucesso: true,
    mensagem,
    desativados: totalCascata + 1,
  };
}

function podeDesativar(perfilAtor, perfilAlvo, ator, alvo) {
  const escopoCondominio = [PERFIS.ADMIN_GERAL, PERFIS.ADMIN_SINDICO];

  if (escopoCondominio.includes(perfilAtor)) {
    return ator.condominioId === alvo.condominioId;
  }

  if (perfilAtor === PERFIS.MORADOR || perfilAtor === PERFIS.DONO_ALUGUEL) {
    if (alvo.cadastradoPorId !== ator.id && alvo.unidadeId !== ator.unidadeId) {
      return false;
    }
    return [PERFIS.MORADOR, PERFIS.CONVIDADO].includes(perfilAlvo);
  }

  return false;
}

async function buscarUsuariosCascata(alvo) {
  const perfil = alvo.getPerfilEfetivo();
  const desativados = [];

  if (perfil === PERFIS.MORADOR) {
    const vinculados = await User.findAll({
      where: {
        cadastradoPorId: alvo.id,
        status: STATUS_USUARIO.ACTIVE,
        perfil: PERFIS.CONVIDADO,
      },
    });
    desativados.push(...vinculados);
  }

  // Desativar o responsável financeiro derruba a unidade inteira.
  if (perfil === PERFIS.MORADOR && alvo.responsavelFinanceiro) {
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
