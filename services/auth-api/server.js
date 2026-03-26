import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import mongoose from 'mongoose';
import helmet from 'helmet';
import session from 'express-session';
import passport from 'passport';
import { Strategy as GoogleStrategy } from 'passport-google-oauth20';
import User from './models/User.js';
import authRoutes from './routes/auth.js';

dotenv.config();

if (!process.env.JWT_SECRET) {
  console.error('ERRO: JWT_SECRET não definido no .env');
  process.exit(1);
}

// Necessário apenas para o fluxo OAuth (state verification) — auth real é JWT
passport.serializeUser((user, done) => done(null, user._id.toString()));
passport.deserializeUser(async (id, done) => {
  try { done(null, await User.findById(id)); } catch (e) { done(e); }
});

if (process.env.GOOGLE_CLIENT_ID && process.env.GOOGLE_CLIENT_SECRET) {
  passport.use(new GoogleStrategy(
    {
      clientID: process.env.GOOGLE_CLIENT_ID,
      clientSecret: process.env.GOOGLE_CLIENT_SECRET,
      callbackURL: process.env.GOOGLE_CALLBACK_URL || 'http://localhost:3001/api/auth/google/callback',
    },
    async (accessToken, refreshToken, profile, done) => {
      console.log('[Google Strategy] Profile recebido:', { id: profile.id, email: profile.emails?.[0]?.value, nome: profile.displayName });
      try {
        let usuario = await User.findOne({ googleId: profile.id });
        if (!usuario) {
          const email = profile.emails?.[0]?.value;
          if (!email) return done(new Error('Email não fornecido pelo Google'), null);

          usuario = await User.findOne({ email });
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
        console.log('[Google Strategy] Usuário:', usuario ? `encontrado (${usuario.email})` : 'criado');
        return done(null, usuario);
      } catch (err) {
        console.error('[Google Strategy] Erro:', err.message);
        return done(err, null);
      }
    }
  ));
  console.log('✓ Google OAuth configurado');
} else {
  console.warn('⚠ GOOGLE_CLIENT_ID/SECRET não configurados — OAuth Google desativado');
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
  cookie: { secure: false, maxAge: 5 * 60 * 1000 }, // 5 min — só para o fluxo OAuth
}));
app.use(passport.initialize());
app.use(passport.session());

app.use('/api/auth', authRoutes);

app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', message: 'Auth API funcionando' });
});

const startServer = async () => {
  const mongoUri = process.env.MONGODB_URI || 'mongodb://localhost:27017/auth_db';

  try {
    await mongoose.connect(mongoUri);
    console.log('✓ MongoDB conectado');
  } catch (err) {
    console.error('Erro ao conectar MongoDB:', err.message);
    console.error('Certifique-se de que o MongoDB está rodando (Docker ou instalação local)');
  }

  app.listen(PORT, () => {
    console.log(`✓ Servidor rodando em http://localhost:${PORT}`);
    if (!mongoose.connection.readyState) {
      console.warn('⚠ Requisições falharão até o MongoDB conectar');
    }
  });
};

startServer();
