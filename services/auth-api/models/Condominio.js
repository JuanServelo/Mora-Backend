import { DataTypes } from 'sequelize';
import sequelize from '../config/database.js';

const Condominio = sequelize.define('Condominio', {
  id: {
    type: DataTypes.STRING(50),
    primaryKey: true,
    allowNull: false,
  },

  nome: {
    type: DataTypes.STRING(200),
    allowNull: false,
    validate: { len: [2, 200] },
  },

  cnpj: {
    type: DataTypes.STRING(18),
    allowNull: true,
  },

  endereco: {
    type: DataTypes.STRING(300),
    allowNull: true,
  },

  telefone: {
    type: DataTypes.STRING(20),
    allowNull: true,
  },

  email: {
    type: DataTypes.STRING(150),
    allowNull: true,
  },

  status: {
    type: DataTypes.ENUM('active', 'inactive'),
    defaultValue: 'active',
  },

  criadoPorId: {
    type: DataTypes.INTEGER,
    allowNull: true,
  },

  tenantId: {
    type: DataTypes.INTEGER,
    field: 'tenant_id',
    allowNull: true,
  },

}, {
  tableName: 'condominios',
  timestamps: true,
  createdAt: 'createdAt',
  updatedAt: 'updatedAt',
});

export default Condominio;
