import { DataTypes } from 'sequelize';
import sequelize from '../config/database.js';

/**
 * RF01 — Gerenciar Planos SaaS.
 * Catálogo de planos da plataforma (conceitualmente `platform.plan` no Banco AUTH).
 */
const Plan = sequelize.define('Plan', {
  id: {
    type: DataTypes.INTEGER,
    autoIncrement: true,
    primaryKey: true,
  },

  name: {
    type: DataTypes.STRING(150),
    allowNull: false,
    unique: true,
    set(value) {
      this.setDataValue('name', value ? String(value).trim() : value);
    },
  },

  maxCondominiums: {
    type: DataTypes.INTEGER,
    field: 'max_condominiums',
    allowNull: false,
  },

  maxUsersPerCondominium: {
    type: DataTypes.INTEGER,
    field: 'max_users_per_condominium',
    allowNull: false,
  },

  monthlyPrice: {
    type: DataTypes.DECIMAL(10, 2),
    field: 'monthly_price',
    allowNull: false,
    defaultValue: 0,
  },

  activeModules: {
    type: DataTypes.JSONB,
    field: 'active_modules',
    allowNull: false,
    defaultValue: [],
  },

  isActive: {
    type: DataTypes.BOOLEAN,
    field: 'is_active',
    allowNull: false,
    defaultValue: true,
  },
}, {
  tableName: 'plans',
  timestamps: true,
  createdAt: 'createdAt',
  updatedAt: 'updatedAt',
});

export default Plan;
