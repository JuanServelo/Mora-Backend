export const PERFIS = {
  SUPER_ADMIN: 'SUPER_ADMIN',
  CONTRACTING_PROPERTY_MANAGER: 'CONTRACTING_PROPERTY_MANAGER',
  CONTRACTING_SYNDIC: 'CONTRACTING_SYNDIC',
  OPERATIONAL_SYNDIC: 'OPERATIONAL_SYNDIC',
  ADMINISTRATOR: 'ADMINISTRATOR',
  DOORMAN: 'DOORMAN',
  REAL_ESTATE_AGENCY: 'REAL_ESTATE_AGENCY',
  RESIDENT_OWNER: 'RESIDENT_OWNER',
  ABSENT_OWNER: 'ABSENT_OWNER',
  LESSEE: 'LESSEE',
  OCCUPANT: 'OCCUPANT',
  GUEST: 'GUEST',
};

export const PERFIS_CONDOMINIO = [
  PERFIS.OPERATIONAL_SYNDIC,
  PERFIS.ADMINISTRATOR,
  PERFIS.DOORMAN,
  PERFIS.REAL_ESTATE_AGENCY,
  PERFIS.RESIDENT_OWNER,
];

export const PERFIS_UNIDADE = [
  PERFIS.LESSEE,
  PERFIS.OCCUPANT,
  PERFIS.GUEST,
];

export const PERFIS_CADASTRO_CONDOMINIO = [
  PERFIS.OPERATIONAL_SYNDIC,
  PERFIS.ADMINISTRATOR,
  PERFIS.DOORMAN,
  PERFIS.REAL_ESTATE_AGENCY,
  PERFIS.RESIDENT_OWNER,
];

/** Perfis que podem cadastrar usuários de nível condomínio */
export const ATORES_CADASTRO_CONDOMINIO = [
  PERFIS.CONTRACTING_PROPERTY_MANAGER,
  PERFIS.CONTRACTING_SYNDIC,
];

/** Matriz: perfil do ator → perfis que pode cadastrar */
export const PERMISSOES_CADASTRO = {
  [PERFIS.CONTRACTING_PROPERTY_MANAGER]: [
    PERFIS.CONTRACTING_SYNDIC,
    PERFIS.OPERATIONAL_SYNDIC,
    PERFIS.ADMINISTRATOR,
    PERFIS.DOORMAN,
    PERFIS.REAL_ESTATE_AGENCY,
    PERFIS.RESIDENT_OWNER,
    PERFIS.ABSENT_OWNER,
    PERFIS.LESSEE,
    PERFIS.OCCUPANT,
  ],
  [PERFIS.CONTRACTING_SYNDIC]: [
    PERFIS.OPERATIONAL_SYNDIC,
    PERFIS.ADMINISTRATOR,
    PERFIS.DOORMAN,
    PERFIS.REAL_ESTATE_AGENCY,
    PERFIS.RESIDENT_OWNER,
    PERFIS.ABSENT_OWNER,
    PERFIS.LESSEE,
    PERFIS.OCCUPANT,
  ],
  [PERFIS.ADMINISTRATOR]: PERFIS_CADASTRO_CONDOMINIO,
  [PERFIS.OPERATIONAL_SYNDIC]: PERFIS_CADASTRO_CONDOMINIO,
  [PERFIS.RESIDENT_OWNER]: [PERFIS.LESSEE, PERFIS.OCCUPANT, PERFIS.GUEST],
  [PERFIS.ABSENT_OWNER]: [PERFIS.LESSEE],
  [PERFIS.LESSEE]: [PERFIS.OCCUPANT, PERFIS.GUEST],
};

/** Perfis que exigem unidadeId no convite */
export const PERFIS_EXIGEM_UNIDADE = [
  PERFIS.RESIDENT_OWNER,
  PERFIS.ABSENT_OWNER,
  PERFIS.LESSEE,
  PERFIS.OCCUPANT,
  PERFIS.GUEST,
];

/** Perfis de nível unidade que exigem nome completo + CPF no pré-cadastro (US-03.5) */
export const PERFIS_REQUEREM_PRECADASTRO = [
  PERFIS.LESSEE,
  PERFIS.OCCUPANT,
  PERFIS.GUEST,
];

export const STATUS_USUARIO = {
  PENDING_ACTIVATION: 'pending_activation',
  ACTIVE: 'active',
  INACTIVE: 'inactive',
};

export const STATUS_CONVITE = {
  PENDING: 'pending',
  USED: 'used',
  EXPIRED: 'expired',
  REVOKED: 'revoked',
};

export const CONDOMINIO_DEFAULT = 'default';

export function podeCadastrarPerfil(perfilAtor, perfilAlvo) {
  const permitidos = PERMISSOES_CADASTRO[perfilAtor];
  return permitidos?.includes(perfilAlvo) ?? false;
}

export function perfilExigeUnidade(perfil) {
  return PERFIS_EXIGEM_UNIDADE.includes(perfil);
}

export function perfilRequerPrecadastro(perfil) {
  return PERFIS_REQUEREM_PRECADASTRO.includes(perfil);
}

export const PERFIS_ACESSO_ADMIN = [
  PERFIS.CONTRACTING_PROPERTY_MANAGER,
  PERFIS.CONTRACTING_SYNDIC,
  PERFIS.OPERATIONAL_SYNDIC,
  PERFIS.ADMINISTRATOR,
];

export const PERFIS_GESTAO_USUARIOS = [
  ...PERFIS_ACESSO_ADMIN,
  PERFIS.RESIDENT_OWNER,
  PERFIS.ABSENT_OWNER,
  PERFIS.LESSEE,
];

export function podeAcessarAdmin(perfil) {
  return PERFIS_ACESSO_ADMIN.includes(perfil);
}

export function podeGerenciarUsuarios(perfil) {
  return PERFIS_GESTAO_USUARIOS.includes(perfil);
}

export function podeGerenciarOcupantes(perfil) {
  return [
    PERFIS.RESIDENT_OWNER,
    PERFIS.ABSENT_OWNER,
    PERFIS.LESSEE,
  ].includes(perfil);
}

export const PERFIS_OCUPANTE_UNIDADE = [
  PERFIS.LESSEE,
  PERFIS.OCCUPANT,
  PERFIS.GUEST,
];

export function isPerfilCondominio(perfil) {
  return [
    PERFIS.CONTRACTING_PROPERTY_MANAGER,
    PERFIS.CONTRACTING_SYNDIC,
    PERFIS.OPERATIONAL_SYNDIC,
    PERFIS.ADMINISTRATOR,
    PERFIS.DOORMAN,
    PERFIS.REAL_ESTATE_AGENCY,
  ].includes(perfil);
}

export function isSuperAdmin(perfil) {
  return perfil === PERFIS.SUPER_ADMIN;
}

/* ===== RF01 / RF02 — Plataforma (Super Admin) ===== */

/** Catálogo de slugs de módulos que um plano SaaS pode habilitar (RF01 — active_modules). */
export const MODULOS_VALIDOS = [
  'property',
  'access',
  'amenity',
  'complaint',
  'financial',
  'governance',
  'chat',
  'content',
  'notification',
  'reporting',
];

export function modulosSaoValidos(modulos) {
  if (!Array.isArray(modulos)) return false;
  return modulos.every((m) => MODULOS_VALIDOS.includes(m));
}

/** Tipos de tenant (RF02) — categorias de contratante da plataforma. */
export const TIPOS_TENANT = {
  PROPERTY_MANAGER: 'PROPERTY_MANAGER',
  SYNDIC: 'SYNDIC',
};

export const TIPOS_TENANT_VALUES = Object.values(TIPOS_TENANT);

/** Status do tenant (RF02). */
export const STATUS_TENANT = {
  ACTIVE: 'active',
  SUSPENDED: 'suspended',
};

export const STATUS_TENANT_VALUES = Object.values(STATUS_TENANT);
