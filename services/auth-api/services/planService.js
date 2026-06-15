import { Op } from 'sequelize';
import { Plan, Tenant } from '../models/index.js';
import {
  MODULOS_VALIDOS,
  modulosSaoValidos,
  STATUS_TENANT,
} from '../constants/perfis.js';

/** Representação pública de um plano para as respostas da API. */
export function planoPublico(plan) {
  if (!plan) return null;
  return {
    id: plan.id,
    name: plan.name,
    maxCondominiums: plan.maxCondominiums,
    maxUsersPerCondominium: plan.maxUsersPerCondominium,
    monthlyPrice: Number(plan.monthlyPrice),
    activeModules: plan.activeModules || [],
    isActive: plan.isActive,
    createdAt: plan.createdAt,
    updatedAt: plan.updatedAt,
  };
}

function campoVazio(valor) {
  return valor === undefined || valor === null || String(valor).trim() === '';
}

/**
 * US-01.5 — Consulta de planos.
 */
export async function listarPlanos() {
  const planos = await Plan.findAll({ order: [['createdAt', 'DESC']] });
  if (planos.length === 0) {
    return { sucesso: true, planos: [], mensagem: 'Nenhum plano cadastrado.' };
  }
  return { sucesso: true, planos: planos.map(planoPublico) };
}

export async function buscarPlano(id) {
  const plano = await Plan.findByPk(id);
  if (!plano) {
    return { sucesso: false, mensagem: 'Plano não encontrado.', status: 404 };
  }
  return { sucesso: true, plano: planoPublico(plano) };
}

/**
 * US-01.1 — Cadastro de plano.
 */
export async function criarPlano(dados) {
  const {
    name,
    maxCondominiums,
    maxUsersPerCondominium,
    monthlyPrice,
    activeModules,
  } = dados;

  // CA-02 — campos obrigatórios
  const erros = {};
  if (campoVazio(name)) erros.name = 'Este campo é obrigatório.';
  if (campoVazio(maxCondominiums)) erros.maxCondominiums = 'Este campo é obrigatório.';
  if (campoVazio(maxUsersPerCondominium)) erros.maxUsersPerCondominium = 'Este campo é obrigatório.';
  if (campoVazio(monthlyPrice)) erros.monthlyPrice = 'Este campo é obrigatório.';
  if (Object.keys(erros).length > 0) {
    return { sucesso: false, mensagem: 'Este campo é obrigatório.', erros, status: 400 };
  }

  // CA-03 — limites inválidos
  if (Number(maxCondominiums) <= 0 || Number(maxUsersPerCondominium) <= 0) {
    return { sucesso: false, mensagem: 'Informe valores maiores que zero.', status: 400 };
  }

  // CA-04 — preço negativo
  if (Number(monthlyPrice) < 0) {
    return { sucesso: false, mensagem: 'O preço não pode ser negativo.', status: 400 };
  }

  // Módulos (US-01.4 reaproveitado)
  const modulos = activeModules ?? [];
  if (!Array.isArray(modulos) || modulos.length === 0) {
    return { sucesso: false, mensagem: 'Selecione um módulo.', status: 400 };
  }
  if (!modulosSaoValidos(modulos)) {
    return { sucesso: false, mensagem: 'Módulo inválido.', status: 400 };
  }

  // Nome único
  const existente = await Plan.findOne({ where: { name: String(name).trim() } });
  if (existente) {
    return { sucesso: false, mensagem: 'Já existe um plano com este nome.', status: 400 };
  }

  const plano = await Plan.create({
    name,
    maxCondominiums: Number(maxCondominiums),
    maxUsersPerCondominium: Number(maxUsersPerCondominium),
    monthlyPrice: Number(monthlyPrice),
    activeModules: modulos,
    isActive: true,
  });

  return {
    sucesso: true,
    mensagem: 'Plano cadastrado com sucesso.',
    plano: planoPublico(plano),
  };
}

/**
 * US-01.2 — Edição de plano.
 */
export async function editarPlano(id, dados) {
  const plano = await Plan.findByPk(id);
  if (!plano) {
    return { sucesso: false, mensagem: 'Plano não encontrado.', status: 404 };
  }

  const {
    name,
    maxCondominiums,
    maxUsersPerCondominium,
    monthlyPrice,
    activeModules,
  } = dados;

  // CA-02 — nome duplicado
  if (name !== undefined) {
    if (campoVazio(name)) {
      return {
        sucesso: false,
        mensagem: 'Este campo é obrigatório.',
        erros: { name: 'Este campo é obrigatório.' },
        status: 400,
      };
    }
    const duplicado = await Plan.findOne({
      where: { name: String(name).trim(), id: { [Op.ne]: plano.id } },
    });
    if (duplicado) {
      return { sucesso: false, mensagem: 'Já existe um plano com este nome.', status: 400 };
    }
  }

  if (maxCondominiums !== undefined || maxUsersPerCondominium !== undefined) {
    const novoMaxCond = maxCondominiums !== undefined
      ? Number(maxCondominiums) : plano.maxCondominiums;
    const novoMaxUsers = maxUsersPerCondominium !== undefined
      ? Number(maxUsersPerCondominium) : plano.maxUsersPerCondominium;

    if (novoMaxCond <= 0 || novoMaxUsers <= 0) {
      return { sucesso: false, mensagem: 'Informe valores maiores que zero.', status: 400 };
    }

    // CA-03 — limite inconsistente com tenants ativos
    const tenantsAtivos = await Tenant.findAll({
      where: { planId: plano.id, status: STATUS_TENANT.ACTIVE },
    });
    const incompativel = tenantsAtivos.some((t) => t.condominiumCount > novoMaxCond);
    if (incompativel) {
      return {
        sucesso: false,
        mensagem: 'Existem tenants que não atendem a este novo limite.',
        status: 400,
      };
    }
  }

  if (monthlyPrice !== undefined && Number(monthlyPrice) < 0) {
    return { sucesso: false, mensagem: 'O preço não pode ser negativo.', status: 400 };
  }

  if (activeModules !== undefined) {
    if (!Array.isArray(activeModules) || activeModules.length === 0) {
      return { sucesso: false, mensagem: 'Selecione um módulo.', status: 400 };
    }
    if (!modulosSaoValidos(activeModules)) {
      return { sucesso: false, mensagem: 'Módulo inválido.', status: 400 };
    }
    plano.activeModules = activeModules;
  }

  if (name !== undefined) plano.name = name;
  if (maxCondominiums !== undefined) plano.maxCondominiums = Number(maxCondominiums);
  if (maxUsersPerCondominium !== undefined) {
    plano.maxUsersPerCondominium = Number(maxUsersPerCondominium);
  }
  if (monthlyPrice !== undefined) plano.monthlyPrice = Number(monthlyPrice);

  await plano.save();

  return {
    sucesso: true,
    mensagem: 'Alteração salva com sucesso.',
    plano: planoPublico(plano),
  };
}

/**
 * US-01.3 — Ativação e desativação de plano.
 */
export async function definirStatusPlano(id, ativar) {
  const plano = await Plan.findByPk(id);
  if (!plano) {
    return { sucesso: false, mensagem: 'Plano não encontrado.', status: 404 };
  }

  if (ativar) {
    if (plano.isActive) {
      return { sucesso: false, mensagem: 'Este plano já está ativo.', status: 400 };
    }
    plano.isActive = true;
    await plano.save();
    return { sucesso: true, mensagem: 'Plano ativado com sucesso.', plano: planoPublico(plano) };
  }

  // CA-02 — plano já desativado
  if (!plano.isActive) {
    return { sucesso: false, mensagem: 'Este plano já está desativado.', status: 400 };
  }
  plano.isActive = false;
  await plano.save();
  return { sucesso: true, mensagem: 'Plano desativado com sucesso.', plano: planoPublico(plano) };
}

/**
 * US-01.4 — Configuração de módulos do plano.
 */
export async function configurarModulos(id, modulos) {
  const plano = await Plan.findByPk(id);
  if (!plano) {
    return { sucesso: false, mensagem: 'Plano não encontrado.', status: 404 };
  }

  // CA-02 — lista vazia
  if (!Array.isArray(modulos) || modulos.length === 0) {
    return { sucesso: false, mensagem: 'Selecione um módulo.', status: 400 };
  }

  // CA-03 — módulo inválido
  if (!modulosSaoValidos(modulos)) {
    return { sucesso: false, mensagem: 'Módulo inválido.', status: 400 };
  }

  plano.activeModules = modulos;
  await plano.save();

  return {
    sucesso: true,
    mensagem: 'Módulos atualizados com sucesso.',
    plano: planoPublico(plano),
  };
}

export { MODULOS_VALIDOS };
