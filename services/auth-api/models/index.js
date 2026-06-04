import User from './User.js';
import Invite from './Invite.js';
import Condominio from './Condominio.js';

User.hasMany(Invite, { foreignKey: 'cadastradoPorId', as: 'convitesEmitidos' });
Invite.belongsTo(User, { foreignKey: 'cadastradoPorId', as: 'cadastradoPor' });

Invite.belongsTo(User, { foreignKey: 'usedByUserId', as: 'usadoPor' });

User.belongsTo(User, { foreignKey: 'cadastradoPorId', as: 'cadastradoPor' });

// Associações com Condominio
Condominio.hasMany(User, { foreignKey: 'condominioId', as: 'usuarios' });
User.belongsTo(Condominio, { foreignKey: 'condominioId', as: 'condominio' });

Condominio.hasMany(Invite, { foreignKey: 'condominioId', as: 'convites' });
Invite.belongsTo(Condominio, { foreignKey: 'condominioId', as: 'condominio' });

export { User, Invite, Condominio };
