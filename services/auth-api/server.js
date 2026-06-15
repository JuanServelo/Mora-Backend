import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import helmet from 'helmet';
import session from 'express-session';
import passport from 'passport';
import path from 'path';
import { fileURLToPath } from 'url';
import { Strategy as GoogleStrategy } from 'passport-google-oauth20';
import sequelize from './config/database.js';
import './models/index.js';
import User from './models/User.js';
import Condominio from './models/Condominio.js';
import Reclamacao from './models/Reclamacao.js';
import authRoutes from './routes/auth.js';
import usersRoutes from './routes/users.js';
import invitesRoutes from './routes/invites.js';
import userManagementRoutes from './routes/user-management.js';
import reclamacoesRoutes from './routes/reclamacoes.js';
import plansRoutes from './routes/plans.js';
import tenantsRoutes from './routes/tenants.js';
import { garantirColunasNovas, migrarUsuariosLegados } from './migrations/migrate-rf03.js';
import { garantirColunasRf07 } from './migrations/migrate-rf07.js';
import { garantirTabelaCondominios } from './migrations/migrate-condominios.js';
import { garantirTabelaPortaria } from './migrations/migrate-portaria.js';
import { PERFIS, STATUS_USUARIO } from './constants/perfis.js';
import { googleOAuthConfigurado } from './utils/oauthConfig.js';
import { conectarBroker, fecharBroker } from './utils/messageBroker.js';

dotenv.config();

const __dirname = path.dirname(fileURLToPath(import.meta.url));

Reclamacao.belongsTo(User, { foreignKey: 'userId', as: 'usuario' });
User.hasMany(Reclamacao, { foreignKey: 'userId', as: 'reclamacoes' });

if (!process.env.JWT_SECRET) {
  console.error('ERRO: JWT_SECRET não definido no .env');
  process.exit(1);
}

passport.serializeUser((user, done) => done(null, String(user.id)));
passport.deserializeUser(async (id, done) => {
  try {
    const u = await User.findByPk(id);
    done(null, u);
  } catch (e) {
    done(e);
  }
});

if (googleOAuthConfigurado()) {
  passport.use(new GoogleStrategy(
    {
      clientID: process.env.GOOGLE_CLIENT_ID,
      clientSecret: process.env.GOOGLE_CLIENT_SECRET,
      callbackURL: process.env.GOOGLE_CALLBACK_URL || 'http://localhost:3001/api/auth/google/callback',
    },
    async (accessToken, refreshToken, profile, done) => {
      try {
        let usuario = await User.findOne({ where: { googleId: profile.id } });
        if (!usuario) {
          const email = profile.emails?.[0]?.value;
          if (!email) return done(new Error('Email não fornecido pelo Google'), null);

          usuario = await User.findOne({ where: { email } });
          if (usuario) {
            if (usuario.status === STATUS_USUARIO.INACTIVE) {
              return done(new Error('Conta desativada'), null);
            }
            if (usuario.status === STATUS_USUARIO.PENDING_ACTIVATION) {
              return done(new Error('Cadastro pendente'), null);
            }
            usuario.googleId = profile.id;
            usuario.provider = 'google';
            await usuario.save();
          } else {
            return done(new Error('Conta não encontrada. Ative via convite.'), null);
          }
        }
        return done(null, usuario);
      } catch (err) {
        return done(err, null);
      }
    },
  ));
  console.log('Google OAuth configurado');
} else {
  console.warn('GOOGLE_CLIENT_ID/SECRET não configurados — OAuth Google desativado');
}

const app = express();
const PORT = process.env.PORT || 3001;

app.use(helmet({ crossOriginResourcePolicy: { policy: 'cross-origin' } }));
app.use(cors({ origin: process.env.FRONTEND_URL || 'http://localhost:5173', credentials: true }));
app.use(express.json());
app.use(session({
  secret: process.env.JWT_SECRET,
  resave: false,
  saveUninitialized: false,
  cookie: { secure: false, maxAge: 5 * 60 * 1000 },
}));
app.use(passport.initialize());
app.use(passport.session());

const uploadDir = process.env.STORAGE_PATH || path.join(__dirname, 'uploads', 'avatars');
app.use('/uploads/avatars', express.static(uploadDir));

app.use('/api/auth', authRoutes);
app.use('/api/invites', invitesRoutes);
app.use('/api/user-management', userManagementRoutes);
app.use('/api/users', usersRoutes);
app.use('/api/reclamacoes', reclamacoesRoutes);
app.use('/api/plans', plansRoutes);
app.use('/api/tenants', tenantsRoutes);

app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', message: 'Auth API funcionando' });
});

const seedAdminUser = async () => {
  const adminEmail = 'admin@mora.com';
  const existing = await User.scope('withPassword').findOne({ where: { role: 'admin' } });
  if (existing) return;
  await User.create({
    nome: 'Admin',
    email: adminEmail,
    senha: 'Vibers@2112',
    role: 'admin',
    perfil: PERFIS.CONTRACTING_PROPERTY_MANAGER,
    status: STATUS_USUARIO.ACTIVE,
    activatedAt: new Date(),
  });
  console.log(`Usuário admin criado: ${adminEmail}`);
};

const startServer = async () => {
  try {
    await sequelize.authenticate();
    console.log('PostgreSQL conectado');
    await garantirColunasNovas();
    await garantirColunasRf07();
    await garantirTabelaCondominios();
    await garantirTabelaPortaria();
    await migrarUsuariosLegados();
    console.log('Tabelas sincronizadas e migrações RF03/RF07/Condomínios aplicadas');
    await seedAdminUser();
  } catch (err) {
    console.error('Erro ao sincronizar tabelas:', err.message);
  }

  // Conecta ao broker de mensageria (não bloqueia o boot se indisponível).
  await conectarBroker();

  const server = app.listen(PORT, () => {
    console.log(`Servidor rodando em http://localhost:${PORT}`);
  });

  server.on('error', (err) => {
    if (err.code === 'EADDRINUSE') {
      console.error(`Porta ${PORT} em uso.`);
      process.exit(1);
    }
    throw err;
  });

  const encerrar = async (sinal) => {
    console.log(`\n[${sinal}] encerrando servidor...`);
    await fecharBroker();
    server.close(() => process.exit(0));
  };
  process.on('SIGINT', () => encerrar('SIGINT'));
  process.on('SIGTERM', () => encerrar('SIGTERM'));
};

startServer();
