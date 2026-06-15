import { PERFIS } from '../constants/perfis.js';

const REDIRECT_MAP = {
  [PERFIS.SUPER_ADMIN]: '/adm/tenants',
  [PERFIS.DOORMAN]: '/portaria',
  [PERFIS.RESIDENT_OWNER]: '/inicio',
  [PERFIS.LESSEE]: '/inicio',
  [PERFIS.OCCUPANT]: '/inicio',
  [PERFIS.GUEST]: '/inicio',
  [PERFIS.CONTRACTING_PROPERTY_MANAGER]: '/inicio',
  [PERFIS.CONTRACTING_SYNDIC]: '/inicio',
  [PERFIS.OPERATIONAL_SYNDIC]: '/inicio',
  [PERFIS.ADMINISTRATOR]: '/inicio',
  [PERFIS.REAL_ESTATE_AGENCY]: '/inicio',
  [PERFIS.ABSENT_OWNER]: '/inicio',
};

export function redirectPorPerfil(perfil) {
  return REDIRECT_MAP[perfil] || '/inicio';
}
