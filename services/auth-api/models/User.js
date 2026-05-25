import { DataTypes } from 'sequelize';
import bcrypt from 'bcryptjs';
import sequelize from '../config/database.js';
import { PERFIS, STATUS_USUARIO } from '../constants/perfis.js';

const PERFIS_VALUES = Object.values(PERFIS);
const STATUS_VALUES = Object.values(STATUS_USUARIO);

const User = sequelize.define('User', {
  id: {
    type: DataTypes.INTEGER,
    autoIncrement: true,
    primaryKey: true,
  },

  nome: {
    type: DataTypes.STRING(150),
    allowNull: true,
    validate: {
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

  telefone: {
    type: DataTypes.STRING(20),
    allowNull: true,
  },

  cpf: {
    type: DataTypes.STRING(14),
    allowNull: true,
  },

  senha: {
    type: DataTypes.STRING,
    allowNull: true,
  },

  fotoUrl: {
    type: DataTypes.STRING(500),
    allowNull: true,
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

  /** @deprecated coluna legada — use perfil */
  role: {
    type: DataTypes.ENUM('user', 'admin'),
    defaultValue: 'user',
  },

  perfil: {
    type: DataTypes.ENUM(...PERFIS_VALUES),
    allowNull: true,
  },

  status: {
    type: DataTypes.ENUM(...STATUS_VALUES),
    defaultValue: STATUS_USUARIO.PENDING_ACTIVATION,
  },

  condominioId: {
    type: DataTypes.STRING(50),
    allowNull: true,
  },

  unidadeId: {
    type: DataTypes.UUID,
    allowNull: true,
  },

  cadastradoPorId: {
    type: DataTypes.INTEGER,
    allowNull: true,
  },

  responsavelFinanceiro: {
    type: DataTypes.BOOLEAN,
    defaultValue: false,
  },

  tokenVersion: {
    type: DataTypes.INTEGER,
    defaultValue: 0,
    allowNull: false,
  },

  activatedAt: {
    type: DataTypes.DATE,
    allowNull: true,
  },

  /** Legado — preferir unidadeId */
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
  if (!this.senha) return false;
  return bcrypt.compare(senhaDigitada, this.senha);
};

User.prototype.getPerfilEfetivo = function getPerfilEfetivo() {
  return this.perfil || PERFIS.RESIDENT_OWNER;
};

export default User;
