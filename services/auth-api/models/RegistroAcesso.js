import { DataTypes } from 'sequelize';
import sequelize from '../config/database.js';

const RegistroAcesso = sequelize.define('RegistroAcesso', {
  id: {
    type: DataTypes.INTEGER,
    autoIncrement: true,
    primaryKey: true,
  },
  usuarioId: {
    type: DataTypes.INTEGER,
    allowNull: false,
  },
  tipo: {
    type: DataTypes.ENUM('ENTRADA', 'SAIDA'),
    allowNull: false,
  },
  registradoPorId: {
    type: DataTypes.INTEGER,
    allowNull: true,
  },
  condominioId: {
    type: DataTypes.STRING(50),
    allowNull: true,
  },
  nomeSnapshot: {
    type: DataTypes.STRING(150),
    allowNull: true,
  },
  perfilSnapshot: {
    type: DataTypes.STRING(50),
    allowNull: true,
  },
  // Turno previsto no momento da entrada. Gravado como cópia para que alterar
  // a escala depois não reescreva retroativamente o que era "dentro do turno".
  turnoPrevisto: {
    type: DataTypes.STRING(50),
    allowNull: true,
  },
  liberacaoExcepcional: {
    type: DataTypes.BOOLEAN,
    allowNull: false,
    defaultValue: false,
  },
}, {
  tableName: 'registros_acesso',
  timestamps: true,
  updatedAt: false,
  createdAt: 'createdAt',
});

export default RegistroAcesso;
