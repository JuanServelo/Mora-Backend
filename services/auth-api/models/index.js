import User from './User.js';
import Invite from './Invite.js';

User.hasMany(Invite, { foreignKey: 'cadastradoPorId', as: 'convitesEmitidos' });
Invite.belongsTo(User, { foreignKey: 'cadastradoPorId', as: 'cadastradoPor' });

Invite.belongsTo(User, { foreignKey: 'usedByUserId', as: 'usadoPor' });

User.belongsTo(User, { foreignKey: 'cadastradoPorId', as: 'cadastradoPor' });

export { User, Invite };
