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
    type: DataTypes.STRING(150),
    allowNull: false,
    validate: {
      notEmpty: { msg: 'Nome é obrigatório' },
      len: {
        args: [2, 150],
        msg: 'Nome deve ter entre 2 e 150 caracteres',
      },
    },
  },

  email: {
    type: DataTypes.STRING(150),
    allowNull: false,
    unique: true,
    validate: {
      isEmail: { msg: 'E-mail inválido' },
      notEmpty: { msg: 'E-mail é obrigatório' },
    },
    set(value) {
      this.setDataValue('email', value?.toLowerCase().trim());
    },
  },

  senha: {
    type: DataTypes.STRING,
    allowNull: true,
    validate: {
      len: {
        args: [6, 255],
        msg: 'Senha deve ter no mínimo 6 caracteres',
      },
    },
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

  bloco: {
    type: DataTypes.STRING(50),
    allowNull: true,
    set(value) {
      this.setDataValue('bloco', value ? String(value).trim() : null);
    },
  },

  apartamento: {
    type: DataTypes.STRING(50),
    allowNull: true,
    set(value) {
      this.setDataValue('apartamento', value ? String(value).trim() : null);
    },
  },

  vaga: {
    type: DataTypes.STRING(50),
    allowNull: true,
    set(value) {
      this.setDataValue('vaga', value ? String(value).trim() : null);
    },
  },

  resetToken: {
    type: DataTypes.STRING,
    allowNull: true,
  },

  resetTokenExpira: {
    type: DataTypes.DATE,
    allowNull: true,
  },

}, {
  tableName: 'users',

  timestamps: true,
  createdAt: 'createdAt',
  updatedAt: 'updatedAt',

  defaultScope: {
    attributes: {
      exclude: ['senha', 'resetToken', 'resetTokenExpira'],
    },
  },

  scopes: {
    withPassword: {
      attributes: { include: ['senha'] },
    },
    withResetToken: {
      attributes: { include: ['resetToken', 'resetTokenExpira'] },
    },
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