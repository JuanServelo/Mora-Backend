import { Op } from 'sequelize';
import { Tenant, Plan } from '../models/index.js';
import {
  TIPOS_TENANT_VALUES,
  STATUS_TENANT,
} from '../constants/perfis.js';
import { planoPublico } from './planService.js';
import { publicarEvento } from '../utils/eventPublisher.js';

/** Representação pública de um tenant para as respostas da API. */
export function tenantPublico(tenant) {
  if (!tenant) return null;
  return {
    id: tenant.id,
    name: tenant.name,
    type: tenant.type,
    cnpj: tenant.cnpj,
    schemaName: tenant.schemaName,
    planId: tenant.planId,
    plano: tenant.plano ? planoPublico(tenant.plano) : undefined,
    status: tenant.status,
    provisioned: tenant.provisioned,
    provisionedAt: tenant.provisionedAt,
    condominiumCount: tenant.condominiumCount,
    createdAt: tenant.createdAt,
    updatedAt: tenant.updatedAt,
  };
}

function campoVazio(valor) {
  return valor === undefined || valor === null || String(valor).trim() === '';
}

/**
 * US-02.6 — Consulta de tenants (listagem).
 */
export async function listarTenants() {
  const tenants = await Tenant.findAll({
    include: [{ model: Plan, as: 'plano' }],
    order: [['createdAt', 'DESC']],
  });
  return { sucesso: true, tenants: tenants.map(tenantPublico) };
}

/**
 * US-02.6 — Consulta de configuração do tenant.
 */
export async function buscarTenant(id) {
  const tenant = await Tenant.findByPk(id, {
    include: [{ model: Plan, as: 'plano' }],
  });
  if (!tenant) {
    return { sucesso: false, mensagem: 'Tenant não encontrado.', status: 404 };
  }
  return { sucesso: true, tenant: tenantPublico(tenant) };
}

/**
 * US-02.1 — Cadastro de novo tenant.
 */
export async function criarTenant(dados) {
  const { name, type, cnpj, schemaName, planId } = dados;

  // CA-04 — plano não selecionado
  if (campoVazio(planId)) {
    return { sucesso: false, mensagem: 'Selecione um plano para continuar.', status: 400 };
  }

  // CA-02 — campos obrigatórios
  const erros = {};
  if (campoVazio(name)) erros.name = 'Este campo é obrigatório.';
  if (campoVazio(type)) erros.type = 'Este campo é obrigatório.';
  if (campoVazio(cnpj)) erros.cnpj = 'Este campo é obrigatório.';
  if (campoVazio(schemaName)) erros.schemaName = 'Este campo é obrigatório.';
  if (Object.keys(erros).length > 0) {
    return { sucesso: false, mensagem: 'Este campo é obrigatório.', erros, status: 400 };
  }

  if (!TIPOS_TENANT_VALUES.includes(type)) {
    return { sucesso: false, mensagem: 'Tipo de tenant inválido.', status: 400 };
  }

  // Plano deve existir e estar ativo
  const plano = await Plan.findByPk(planId);
  if (!plano) {
    return { sucesso: false, mensagem: 'Selecione um plano para continuar.', status: 400 };
  }
  if (!plano.isActive) {
    return { sucesso: false, mensagem: 'Selecione um plano ativo.', status: 400 };
  }

  // CA-03 — CNPJ duplicado
  const cnpjExistente = await Tenant.findOne({ where: { cnpj: String(cnpj).trim() } });
  if (cnpjExistente) {
    return { sucesso: false, mensagem: 'Já existe um tenant com este CNPJ.', status: 400 };
  }

  // schema_name único
  const schemaExistente = await Tenant.findOne({
    where: { schemaName: String(schemaName).trim().toLowerCase() },
  });
  if (schemaExistente) {
    return {
      sucesso: false,
      mensagem: 'Já existe um tenant com este identificador de schema.',
      status: 400,
    };
  }

  const tenant = await Tenant.create({
    name,
    type,
    cnpj: String(cnpj).trim(),
    schemaName,
    planId,
    status: STATUS_TENANT.ACTIVE,
    provisioned: false,
  });

  const completo = await Tenant.findByPk(tenant.id, {
    include: [{ model: Plan, as: 'plano' }],
  });

  return {
    sucesso: true,
    mensagem: 'Tenant cadastrado com sucesso.',
    tenant: tenantPublico(completo),
  };
}

/**
 * US-02.2 — Edição de tenant.
 */
export async function editarTenant(id, dados) {
  const tenant = await Tenant.findByPk(id);
  if (!tenant) {
    return { sucesso: false, mensagem: 'Tenant não encontrado.', status: 404 };
  }

  const { name, type, cnpj, status } = dados;

  if (name !== undefined && campoVazio(name)) {
    return {
      sucesso: false,
      mensagem: 'Este campo é obrigatório.',
      erros: { name: 'Este campo é obrigatório.' },
      status: 400,
    };
  }

  if (type !== undefined && !TIPOS_TENANT_VALUES.includes(type)) {
    return { sucesso: false, mensagem: 'Tipo de tenant inválido.', status: 400 };
  }

  // CA-02 — CNPJ duplicado
  if (cnpj !== undefined) {
    if (campoVazio(cnpj)) {
      return {
        sucesso: false,
        mensagem: 'Este campo é obrigatório.',
        erros: { cnpj: 'Este campo é obrigatório.' },
        status: 400,
      };
    }
    const duplicado = await Tenant.findOne({
      where: { cnpj: String(cnpj).trim(), id: { [Op.ne]: tenant.id } },
    });
    if (duplicado) {
      return { sucesso: false, mensagem: 'Já existe um tenant com este CNPJ.', status: 400 };
    }
  }

  if (status !== undefined && !Object.values(STATUS_TENANT).includes(status)) {
    return { sucesso: false, mensagem: 'Status de tenant inválido.', status: 400 };
  }

  if (name !== undefined) tenant.name = name;
  if (type !== undefined) tenant.type = type;
  if (cnpj !== undefined) tenant.cnpj = String(cnpj).trim();
  if (status !== undefined) tenant.status = status;

  await tenant.save();

  const completo = await Tenant.findByPk(tenant.id, {
    include: [{ model: Plan, as: 'plano' }],
  });

  return {
    sucesso: true,
    mensagem: 'Alteração salva com sucesso.',
    tenant: tenantPublico(completo),
  };
}

/**
 * US-02.3 — Provisionamento inicial do tenant.
 */
export async function provisionarTenant(id) {
  const tenant = await Tenant.findByPk(id);
  if (!tenant) {
    return { sucesso: false, mensagem: 'Tenant não encontrado.', status: 404 };
  }

  // CA-02 — tenant já provisionado
  if (tenant.provisioned) {
    return { sucesso: false, mensagem: 'Este tenant já foi provisionado.', status: 400 };
  }

  // CA-01 — publica o evento de domínio para o Banco MORA
  const resultadoEvento = await publicarEvento('tenant.provisioned', {
    tenantId: tenant.id,
    schemaName: tenant.schemaName,
    name: tenant.name,
    planId: tenant.planId,
  });

  // CA-03 — falha no fluxo de mensageria
  if (!resultadoEvento.sucesso) {
    return {
      sucesso: false,
      mensagem: 'Não foi possível concluir o provisionamento do tenant.',
      status: 500,
    };
  }

  tenant.provisioned = true;
  tenant.provisionedAt = new Date();
  await tenant.save();

  return {
    sucesso: true,
    mensagem: 'Tenant provisionado com sucesso.',
    tenant: tenantPublico(tenant),
  };
}

/**
 * US-02.4 — Suspensão de tenant.
 */
export async function suspenderTenant(id) {
  const tenant = await Tenant.findByPk(id);
  // CA-02 — tenant inexistente
  if (!tenant) {
    return { sucesso: false, mensagem: 'Tenant não encontrado.', status: 404 };
  }

  // CA-03 — tenant já suspenso
  if (tenant.status === STATUS_TENANT.SUSPENDED) {
    return { sucesso: false, mensagem: 'Este tenant já está suspenso.', status: 400 };
  }

  tenant.status = STATUS_TENANT.SUSPENDED;
  await tenant.save();

  await publicarEvento('tenant.suspended', {
    tenantId: tenant.id,
    schemaName: tenant.schemaName,
  });

  return {
    sucesso: true,
    mensagem: 'Tenant suspenso com sucesso.',
    tenant: tenantPublico(tenant),
  };
}

/**
 * Reativação de tenant suspenso (complementa US-02.4).
 */
export async function reativarTenant(id) {
  const tenant = await Tenant.findByPk(id);
  if (!tenant) {
    return { sucesso: false, mensagem: 'Tenant não encontrado.', status: 404 };
  }
  if (tenant.status === STATUS_TENANT.ACTIVE) {
    return { sucesso: false, mensagem: 'Este tenant já está ativo.', status: 400 };
  }
  tenant.status = STATUS_TENANT.ACTIVE;
  await tenant.save();
  return {
    sucesso: true,
    mensagem: 'Tenant reativado com sucesso.',
    tenant: tenantPublico(tenant),
  };
}

/**
 * US-02.5 — Gerenciamento de plano do tenant.
 */
export async function alterarPlanoTenant(id, planId) {
  const tenant = await Tenant.findByPk(id);
  if (!tenant) {
    return { sucesso: false, mensagem: 'Tenant não encontrado.', status: 404 };
  }

  if (campoVazio(planId)) {
    return { sucesso: false, mensagem: 'Selecione um plano para continuar.', status: 400 };
  }

  const plano = await Plan.findByPk(planId);
  if (!plano) {
    return { sucesso: false, mensagem: 'Selecione um plano para continuar.', status: 400 };
  }

  // CA-03 — plano inativo
  if (!plano.isActive) {
    return { sucesso: false, mensagem: 'Selecione um plano ativo.', status: 400 };
  }

  // CA-02 — excedente de condomínios
  if (tenant.condominiumCount > plano.maxCondominiums) {
    return {
      sucesso: false,
      mensagem: 'O plano selecionado não comporta a quantidade atual de condomínios.',
      status: 400,
    };
  }

  tenant.planId = plano.id;
  await tenant.save();

  const completo = await Tenant.findByPk(tenant.id, {
    include: [{ model: Plan, as: 'plano' }],
  });

  return {
    sucesso: true,
    mensagem: 'Plano alterado com sucesso.',
    tenant: tenantPublico(completo),
  };
}
