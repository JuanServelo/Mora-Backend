import { DataTypes } from 'sequelize';
import bcrypt from 'bcryptjs';
import sequelize from '../config/database.js';
import { PERFIS, STATUS_USUARIO } from '../constants/perfis.js';

const PERFIS_VALUES = Object.values(PERFIS);
const STATUS_VALUES = Object.values(STATUS_USUARIO);

/** Campos sensíveis: nunca saem do banco sem um scope explícito. */
const SEGREDOS = [
  'senha',
  'resetToken',
  'resetTokenExpira',
  'oauthCode',
  'oauthCodeExpira',
];

/**
 * Monta um scope por exclusão. Listar o que sai — em vez de `include`, que
 * depende da ordem de merge com o defaultScope — deixa determinístico quais
 * campos a consulta traz.
 */
const semSegredosExceto = (...manter) => ({
  attributes: { exclude: SEGREDOS.filter((campo) => !manter.includes(campo)) },
});

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
    allowNull: true,
    unique: true,
    validate: {
      isEmailOrEmpty(value) {
        if (value == null || value === '') return;
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(value)) {
          throw new Error('E-mail inválido');
        }
      },
    },
    set(value) {
      this.setDataValue('email', value ? value.toLowerCase().trim() : null);
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

  dataNascimento: {
    type: DataTypes.DATEONLY,
    allowNull: true,
  },

  semAcessoSistema: {
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

  resetToken: {
    type: DataTypes.STRING,
    allowNull: true,
  },

  resetTokenExpira: {
    type: DataTypes.DATE,
    allowNull: true,
  },

  // Código de uso único trocado por JWT no fim do fluxo OAuth, para que o
  // token não trafegue na URL do redirect.
  oauthCode: {
    type: DataTypes.STRING,
    allowNull: true,
  },

  oauthCodeExpira: {
    type: DataTypes.DATE,
    allowNull: true,
  },

}, {
  tableName: 'users',
  timestamps: true,
  createdAt: 'createdAt',
  updatedAt: 'updatedAt',

  defaultScope: semSegredosExceto(),

  scopes: {
    withPassword: semSegredosExceto('senha'),
    withResetToken: semSegredosExceto('resetToken', 'resetTokenExpira'),
    withOauthCode: semSegredosExceto('oauthCode', 'oauthCodeExpira'),
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
  return this.perfil || PERFIS.MORADOR;
};

/**
 * Revoga todos os JWT já emitidos para este usuário. O middleware compara a
 * claim `tokenVersion` com esta coluna, então incrementar derruba as sessões.
 */
User.prototype.revogarTokens = async function revogarTokens() {
  this.tokenVersion = (this.tokenVersion || 0) + 1;
  await this.save({ validate: false });
  return this.tokenVersion;
};

export default User;
