import { DataTypes } from 'sequelize';
import sequelize from '../config/database.js';
import {
  TIPOS_TENANT_VALUES,
  STATUS_TENANT_VALUES,
  STATUS_TENANT,
} from '../constants/perfis.js';

/**
 * RF02 — Gerenciar Tenant.
 * Cliente contratante da plataforma (conceitualmente `platform.tenant` no Banco AUTH).
 */
const Tenant = sequelize.define('Tenant', {
  id: {
    type: DataTypes.INTEGER,
    autoIncrement: true,
    primaryKey: true,
  },

  name: {
    type: DataTypes.STRING(150),
    allowNull: false,
    set(value) {
      this.setDataValue('name', value ? String(value).trim() : value);
    },
  },

  type: {
    type: DataTypes.ENUM(...TIPOS_TENANT_VALUES),
    allowNull: false,
  },

  cnpj: {
    type: DataTypes.STRING(18),
    allowNull: false,
    unique: true,
  },

  schemaName: {
    type: DataTypes.STRING(63),
    field: 'schema_name',
    allowNull: false,
    unique: true,
    set(value) {
      this.setDataValue('schemaName', value ? String(value).trim().toLowerCase() : value);
    },
  },

  planId: {
    type: DataTypes.INTEGER,
    field: 'plan_id',
    allowNull: false,
  },

  status: {
    type: DataTypes.ENUM(...STATUS_TENANT_VALUES),
    allowNull: false,
    defaultValue: STATUS_TENANT.ACTIVE,
  },

  provisioned: {
    type: DataTypes.BOOLEAN,
    allowNull: false,
    defaultValue: false,
  },

  provisionedAt: {
    type: DataTypes.DATE,
    field: 'provisioned_at',
    allowNull: true,
  },

  /**
   * Quantidade atual de condomínios do tenant. Vive no Banco MORA, mas é mantida
   * aqui (consistência eventual) para validar limites de plano (US-02.5 CA-02).
   */
  condominiumCount: {
    type: DataTypes.INTEGER,
    field: 'condominium_count',
    allowNull: false,
    defaultValue: 0,
  },
}, {
  tableName: 'tenants',
  timestamps: true,
  createdAt: 'createdAt',
  updatedAt: 'updatedAt',
});

export default Tenant;
