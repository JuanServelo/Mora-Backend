import User from './User.js';
import Invite from './Invite.js';
import Plan from './Plan.js';
import Tenant from './Tenant.js';

User.hasMany(Invite, { foreignKey: 'cadastradoPorId', as: 'convitesEmitidos' });
Invite.belongsTo(User, { foreignKey: 'cadastradoPorId', as: 'cadastradoPor' });

Invite.belongsTo(User, { foreignKey: 'usedByUserId', as: 'usadoPor' });

User.belongsTo(User, { foreignKey: 'cadastradoPorId', as: 'cadastradoPor' });

// RF01 / RF02 — Plataforma
Plan.hasMany(Tenant, { foreignKey: 'planId', as: 'tenants' });
Tenant.belongsTo(Plan, { foreignKey: 'planId', as: 'plano' });
Tenant.hasMany(User, { foreignKey: 'tenantId', as: 'usuarios' });
User.belongsTo(Tenant, { foreignKey: 'tenantId', as: 'tenant' });

export { User, Invite, Plan, Tenant };
