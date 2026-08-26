import { DataTypes } from 'sequelize';
import sequelize from '../config/database.js';
import { PERFIS, STATUS_CONVITE } from '../constants/perfis.js';

const PERFIS_VALUES = Object.values(PERFIS);
const STATUS_VALUES = Object.values(STATUS_CONVITE);

const Invite = sequelize.define('Invite', {
  id: {
    type: DataTypes.INTEGER,
    autoIncrement: true,
    primaryKey: true,
  },

  codigo: {
    type: DataTypes.STRING(12),
    allowNull: false,
    unique: true,
  },

  email: {
    type: DataTypes.STRING(150),
    allowNull: false,
    set(value) {
      this.setDataValue('email', value?.toLowerCase().trim());
    },
  },

  perfil: {
    type: DataTypes.ENUM(...PERFIS_VALUES),
    allowNull: false,
  },

  condominioId: {
    type: DataTypes.STRING(50),
    allowNull: false,
  },

  unidadeId: {
    type: DataTypes.UUID,
    allowNull: true,
  },

  nomePrecadastro: {
    type: DataTypes.STRING(150),
    allowNull: true,
  },

  cpfPrecadastro: {
    type: DataTypes.STRING(14),
    allowNull: true,
  },

  cadastradoPorId: {
    type: DataTypes.INTEGER,
    allowNull: false,
  },

  /**
   * Marca o convite do morador que responde financeiramente pela unidade.
   * Com a simplificação de perfis, é o que distingue o antigo Lessee do
   * Occupant — ambos viram MORADOR.
   */
  responsavelFinanceiro: {
    type: DataTypes.BOOLEAN,
    allowNull: false,
    defaultValue: false,
  },

  status: {
    type: DataTypes.ENUM(...STATUS_VALUES),
    defaultValue: STATUS_CONVITE.PENDING,
  },

  expiresAt: {
    type: DataTypes.DATE,
    allowNull: false,
  },

  usedAt: {
    type: DataTypes.DATE,
    allowNull: true,
  },

  usedByUserId: {
    type: DataTypes.INTEGER,
    allowNull: true,
  },

}, {
  tableName: 'invites',
  timestamps: true,
  createdAt: 'createdAt',
  updatedAt: 'updatedAt',
});

export default Invite;
