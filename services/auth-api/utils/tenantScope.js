import Condominio from '../models/Condominio.js';
import { PERFIS } from '../constants/perfis.js';

export function isGerenteTenant(perfil) {
  return [
    PERFIS.CONTRACTING_PROPERTY_MANAGER,
    PERFIS.CONTRACTING_SYNDIC,
  ].includes(perfil);
}

export async function condominioIdsDoTenant(tenantId) {
  if (!tenantId) return [];
  const rows = await Condominio.findAll({
    where: { tenantId },
    attributes: ['id'],
  });
  return rows.map((r) => r.id);
}

export async function condominioPertenceAoTenant(condominioId, tenantId) {
  if (!tenantId || !condominioId) return false;
  const count = await Condominio.count({ where: { id: condominioId, tenantId } });
  return count > 0;
}

export async function escopoTenantDoAtor(ator) {
  const perfil = ator.getPerfilEfetivo?.() ?? ator.perfil;
  if (!isGerenteTenant(perfil) || !ator.tenantId) return null;
  const condIds = await condominioIdsDoTenant(ator.tenantId);
  return { tenantId: ator.tenantId, condIds };
}
