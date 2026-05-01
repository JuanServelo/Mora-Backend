import { DataTypes } from 'sequelize';
import bcrypt from 'bcryptjs';
import sequelize from '../config/database.js';

const User = sequelize.define('User', {
  id: {
    type: DataTypes.INTEGER,
    autoIncrement: true,
    primaryKey: true,
  },
  nome: {
    type: DataTypes.STRING,
    allowNull: false,
    validate: { notEmpty: { msg: 'Nome é obrigatório' } },
  },
  email: {
    type: DataTypes.STRING,
    allowNull: false,
    unique: true,
    validate: { isEmail: true },
    set(value) {
      this.setDataValue('email', value?.toLowerCase().trim());
    },
  },
  senha: {
    type: DataTypes.STRING,
    validate: { len: [6, 255] },
  },
  googleId: {
    type: DataTypes.STRING,
    allowNull: true,
    unique: true,
  },
  provider: {
    type: DataTypes.ENUM('local', 'google'),
    defaultValue: 'local',
  },
  role: {
    type: DataTypes.ENUM('user', 'admin'),
    defaultValue: 'user',
  },
  bloco: { type: DataTypes.STRING, allowNull: true },
  apartamento: { type: DataTypes.STRING, allowNull: true },
  vaga: { type: DataTypes.STRING, allowNull: true },
  resetToken: { type: DataTypes.STRING, allowNull: true },
  resetTokenExpira: { type: DataTypes.DATE, allowNull: true },
}, {
  tableName: 'users',
  timestamps: true,
  createdAt: 'createdAt',
  updatedAt: 'updatedAt',
  defaultScope: {
    attributes: { exclude: ['senha', 'resetToken', 'resetTokenExpira'] },
  },
  scopes: {
    withPassword: { attributes: { include: ['senha'] } },
    withResetToken: { attributes: { include: ['resetToken', 'resetTokenExpira'] } },
  },
  hooks: {
    beforeSave: async (user) => {
      if (user.changed('senha') && user.senha) {
        user.senha = await bcrypt.hash(user.senha, 10);
      }
    },
  },
});

User.prototype.compararSenha = async function compararSenha(senhaDigitada) {
  return bcrypt.compare(senhaDigitada, this.senha);
};

export default User;
