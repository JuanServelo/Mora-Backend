import { Op } from 'sequelize';
import User from '../models/User.js';
import Invite from '../models/Invite.js';
import {
  PERFIS,
  STATUS_USUARIO,
  STATUS_CONVITE,
  PERFIS_OCUPANTE_UNIDADE,
  podeCadastrarPerfil,
  podeGerenciarOcupantes,
} from '../constants/perfis.js';
import { validarUnidadeExiste } from '../utils/portariaClient.js';
import { normalizarCpf } from '../utils/usuarioPublico.js';
import { temIdadeMinima } from '../utils/ageValidation.js';
import { criarConvite, lesseeAtivoNaUnidade } from './inviteService.js';
import { desativarUsuario } from './userManagementService.js';

import { MSG_RF07 as MSG } from '../constants/occupantMessages.js';

function validarCampos(dados, campos) {
  const erros = {};
  for (const campo of campos) {
    const valor = dados[campo];
    if (valor === undefined || valor === null || String(valor).trim() === '') {
      erros[campo] = MSG.CAMPO_OBRIGATORIO;
    }
  }
  if (Object.keys(erros).length > 0) {
    return { sucesso: false, mensagem: MSG.CAMPO_OBRIGATORIO, erros, status: 400 };
  }
  return null;
}

async function assertAcessoUnidade(ator, unidadeId) {
  if (!unidadeId) {
    return { sucesso: false, mensagem: MSG.UNIDADE_INVALIDA, status: 400 };
  }
  const ok = await validarUnidadeExiste(unidadeId);
  if (!ok) {
    return { sucesso: false, mensagem: MSG.UNIDADE_INVALIDA, status: 404 };
  }
  const perfil = ator.getPerfilEfetivo();
  if (podeGerenciarOcupantes(perfil) && ator.unidadeId !== unidadeId) {
    return { sucesso: false, mensagem: MSG.SEM_PERMISSAO_CADASTRO, status: 403 };
  }
  return null;
}

async function cpfEmUsoNaUnidade(cpf, unidadeId) {
  const cpfNorm = normalizarCpf(cpf);
  if (!cpfNorm) return false;
  const existe = await User.findOne({
    where: {
      unidadeId,
      cpf: cpfNorm,
      status: STATUS_USUARIO.ACTIVE,
    },
  });
  return !!existe;
}

function mapUsuarioOcupante(u) {
  return {
    id: u.id,
    nome: u.nome,
    email: u.email,
    cpf: u.cpf,
    perfil: u.getPerfilEfetivo(),
    status: u.status,
    responsavelFinanceiro: u.responsavelFinanceiro ?? false,
    semAcessoSistema: u.semAcessoSistema ?? false,
    dataNascimento: u.dataNascimento,
    cadastradoPorId: u.cadastradoPorId,
    tipo: 'usuario',
  };
}

export async function listarOcupantesUnidade(ator, unidadeId) {
  const bloqueio = await assertAcessoUnidade(ator, unidadeId);
  if (bloqueio) return bloqueio;

  const [usuarios, convitesPendentes] = await Promise.all([
    User.findAll({
      where: {
        unidadeId,
        status: STATUS_USUARIO.ACTIVE,
        perfil: { [Op.in]: [...PERFIS_OCUPANTE_UNIDADE, PERFIS.RESIDENT_OWNER, PERFIS.ABSENT_OWNER] },
      },
      order: [['nome', 'ASC']],
    }),
    Invite.findAll({
      where: {
        unidadeId,
        status: STATUS_CONVITE.PENDING,
        expiresAt: { [Op.gt]: new Date() },
        perfil: { [Op.in]: [PERFIS.LESSEE, PERFIS.OCCUPANT] },
      },
      order: [['createdAt', 'DESC']],
    }),
  ]);

  const responsavel = usuarios.find((u) => u.responsavelFinanceiro);
  const ocupantes = usuarios
    .filter((u) => PERFIS_OCUPANTE_UNIDADE.includes(u.getPerfilEfetivo()))
    .map(mapUsuarioOcupante);

  return {
    sucesso: true,
    responsavelFinanceiroId: responsavel?.id ?? null,
    ocupantes,
    convitesPendentes: convitesPendentes.map((c) => ({
      id: c.id,
      email: c.email,
      perfil: c.perfil,
      nomePrecadastro: c.nomePrecadastro,
      cpfPrecadastro: c.cpfPrecadastro,
      expiresAt: c.expiresAt,
      status: 'pendente',
      tipo: 'convite',
    })),
    mensagemVazio: ocupantes.length === 0 && convitesPendentes.length === 0
      ? MSG.NENHUM_OCUPANTE
      : null,
  };
}

async function emitirConviteOcupante(ator, unidadeId, perfil, dados) {
  const perfilAtor = ator.getPerfilEfetivo();
  if (!podeCadastrarPerfil(perfilAtor, perfil)) {
    return { sucesso: false, mensagem: MSG.SEM_PERMISSAO_CADASTRO, status: 403 };
  }

  if (
    perfil === PERFIS.LESSEE
    && (perfilAtor === PERFIS.ABSENT_OWNER || perfilAtor === PERFIS.RESIDENT_OWNER)
  ) {
    const lesseeExistente = await lesseeAtivoNaUnidade(unidadeId);
    if (lesseeExistente) {
      return { sucesso: false, mensagem: MSG.LESSEE_DUPLICADO, status: 400 };
    }
  }

  const cpfNorm = normalizarCpf(dados.cpf);
  if (await cpfEmUsoNaUnidade(cpfNorm, unidadeId)) {
    return { sucesso: false, mensagem: MSG.CPF_EM_USO, status: 400 };
  }

  const { invite, emailEnviado, avisoEmail } = await criarConvite({
    email: dados.email.toLowerCase().trim(),
    perfil,
    cadastradoPorId: ator.id,
    condominioId: ator.condominioId,
    unidadeId,
    nomePrecadastro: dados.nome.trim(),
    cpfPrecadastro: cpfNorm,
  });

  const mensagemSucesso = perfil === PERFIS.LESSEE ? MSG.LESSEE_SUCESSO : MSG.OCCUPANT_SUCESSO;

  return {
    sucesso: true,
    mensagem: mensagemSucesso,
    emailEnviado,
    avisoEmail: avisoEmail || null,
    convite: {
      id: invite.id,
      email: invite.email,
      perfil: invite.perfil,
      codigo: invite.codigo,
      expiresAt: invite.expiresAt,
    },
  };
}

export async function cadastrarLessee(ator, unidadeId, dados) {
  const bloqueio = await assertAcessoUnidade(ator, unidadeId);
  if (bloqueio) return bloqueio;

  const campos = validarCampos(dados, ['nome', 'cpf', 'email']);
  if (campos) return campos;

  return emitirConviteOcupante(ator, unidadeId, PERFIS.LESSEE, dados);
}

export async function cadastrarOccupant(ator, unidadeId, dados) {
  const bloqueio = await assertAcessoUnidade(ator, unidadeId);
  if (bloqueio) return bloqueio;

  // CA-04: Absent Owner não pode cadastrar Occupant
  if (ator.getPerfilEfetivo() === PERFIS.ABSENT_OWNER) {
    return {
      sucesso: false,
      mensagem: 'Somente Resident Owner ou Lessee podem cadastrar Occupants.',
      status: 403,
    };
  }

  const campos = validarCampos(dados, ['nome', 'cpf', 'email', 'dataNascimento']);
  if (campos) return campos;

  if (!temIdadeMinima(dados.dataNascimento, 16)) {
    return { sucesso: false, mensagem: MSG.OCCUPANT_MENOR, status: 400 };
  }

  return emitirConviteOcupante(ator, unidadeId, PERFIS.OCCUPANT, dados);
}

export async function cadastrarGuest(ator, unidadeId, dados) {
  const bloqueio = await assertAcessoUnidade(ator, unidadeId);
  if (bloqueio) return bloqueio;

  const perfilAtor = ator.getPerfilEfetivo();
  if (!podeCadastrarPerfil(perfilAtor, PERFIS.GUEST)) {
    return { sucesso: false, mensagem: MSG.SEM_PERMISSAO_CADASTRO, status: 403 };
  }

  const campos = validarCampos(dados, ['nome', 'cpf']);
  if (campos) return campos;

  const cpfNorm = normalizarCpf(dados.cpf);
  if (await cpfEmUsoNaUnidade(cpfNorm, unidadeId)) {
    return { sucesso: false, mensagem: MSG.CPF_EM_USO, status: 400 };
  }

  const usuario = await User.create({
    nome: dados.nome.trim(),
    email: null,
    cpf: cpfNorm,
    dataNascimento: dados.dataNascimento || null,
    perfil: PERFIS.GUEST,
    status: STATUS_USUARIO.ACTIVE,
    semAcessoSistema: true,
    condominioId: ator.condominioId,
    unidadeId,
    cadastradoPorId: ator.id,
    provider: 'local',
    activatedAt: new Date(),
    tokenVersion: 0,
  });

  return {
    sucesso: true,
    mensagem: MSG.GUEST_SUCESSO,
    usuario: mapUsuarioOcupante(usuario),
  };
}

export async function verificarElegibilidadeTransferencia(ator, unidadeId) {
  const bloqueio = await assertAcessoUnidade(ator, unidadeId);
  if (bloqueio) return bloqueio;

  const perfil = ator.getPerfilEfetivo();
  const lessee = await lesseeAtivoNaUnidade(unidadeId);

  const podeTransferir = perfil === PERFIS.RESIDENT_OWNER
    && ator.responsavelFinanceiro
    && ator.unidadeId === unidadeId;

  if (!lessee) {
    return {
      sucesso: true,
      elegivel: false,
      mensagem: MSG.TRANSFER_CADASTRE_LESSEE,
      podeTransferir: false,
    };
  }

  if (!podeTransferir) {
    return {
      sucesso: true,
      elegivel: false,
      mensagem: MSG.TRANSFER_SEM_PERMISSAO,
      podeTransferir: false,
    };
  }

  return {
    sucesso: true,
    elegivel: true,
    podeTransferir: true,
    lesseeId: lessee.id,
  };
}

export async function transferirResponsabilidadeFinanceira(ator, unidadeId) {
  const elegibilidade = await verificarElegibilidadeTransferencia(ator, unidadeId);
  if (!elegibilidade.sucesso) return elegibilidade;

  if (!elegibilidade.podeTransferir) {
    const msg = elegibilidade.mensagem === MSG.TRANSFER_CADASTRE_LESSEE
      ? MSG.TRANSFER_SEM_LESSEE
      : elegibilidade.mensagem;
    return { sucesso: false, mensagem: msg, status: 400 };
  }

  const lessee = await lesseeAtivoNaUnidade(unidadeId);
  if (!lessee) {
    return { sucesso: false, mensagem: MSG.TRANSFER_SEM_LESSEE, status: 400 };
  }

  ator.responsavelFinanceiro = false;
  ator.perfil = PERFIS.ABSENT_OWNER;
  await ator.save();

  lessee.responsavelFinanceiro = true;
  await lessee.save();

  const vinculados = await User.findAll({
    where: {
      unidadeId,
      cadastradoPorId: ator.id,
      status: STATUS_USUARIO.ACTIVE,
      perfil: { [Op.in]: [PERFIS.OCCUPANT, PERFIS.GUEST] },
    },
  });

  for (const u of vinculados) {
    u.status = STATUS_USUARIO.INACTIVE;
    u.tokenVersion = (u.tokenVersion || 0) + 1;
    await u.save();
  }

  return {
    sucesso: true,
    mensagem: MSG.TRANSFER_SUCESSO,
    desativados: vinculados.length,
  };
}

export async function removerVinculo(ator, unidadeId, userId) {
  const bloqueio = await assertAcessoUnidade(ator, unidadeId);
  if (bloqueio) return bloqueio;

  const alvo = await User.findByPk(userId);
  if (!alvo || alvo.unidadeId !== unidadeId || alvo.status !== STATUS_USUARIO.ACTIVE) {
    return { sucesso: false, mensagem: MSG.VINCULO_INEXISTENTE, status: 404 };
  }

  const resultado = await desativarUsuario(ator, userId, { mensagemRemocao: true });
  return resultado;
}
