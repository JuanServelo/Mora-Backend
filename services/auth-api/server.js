import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import helmet from 'helmet';
import session from 'express-session';
import passport from 'passport';
import { Strategy as GoogleStrategy } from 'passport-google-oauth20';
import sequelize from './config/database.js';
import User from './models/User.js';
import Reclamacao from './models/Reclamacao.js';
import authRoutes from './routes/auth.js';
import usersRoutes from './routes/users.js';
import reclamacoesRoutes from './routes/reclamacoes.js';

dotenv.config();

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

if (process.env.GOOGLE_CLIENT_ID && process.env.GOOGLE_CLIENT_SECRET) {
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
            usuario.googleId = profile.id;
            usuario.provider = 'google';
            await usuario.save();
          } else {
            usuario = await User.create({
              nome: profile.displayName,
              email,
              googleId: profile.id,
              provider: 'google',
            });
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

app.use(helmet());
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

app.use('/api/auth', authRoutes);
app.use('/api/users', usersRoutes);
app.use('/api/reclamacoes', reclamacoesRoutes);

app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', message: 'Auth API funcionando' });
});

const startServer = async () => {
  try {
    await sequelize.authenticate();
    console.log('PostgreSQL conectado');
    await sequelize.sync({ alter: true });
    console.log('Tabelas sincronizadas');
  } catch (err) {
    console.error('Erro ao sincronizar tabelas:', err.message);
  }

  try {
    await sequelize.query(`
      ALTER TABLE users
        ADD COLUMN IF NOT EXISTS bloco       VARCHAR(50),
        ADD COLUMN IF NOT EXISTS apartamento VARCHAR(50),
        ADD COLUMN IF NOT EXISTS vaga        VARCHAR(50)
    `);
    console.log('Colunas de vínculo garantidas');
  } catch (err) {
    console.error('Aviso ao garantir colunas de vínculo:', err.message);
  }

  const server = app.listen(PORT, () => {
    console.log(`Servidor rodando em http://localhost:${PORT}`);
  });

  server.on('error', (err) => {
    if (err.code === 'EADDRINUSE') {
      console.error(
        `Porta ${PORT} em uso. Feche o outro \`npm run dev\` ou defina outra PORT no .env (ex.: 3002). No Windows: netstat -ano | findstr :${PORT} e encerre o PID.`
      );
      process.exit(1);
    }
    throw err;
  });
};

startServer();
