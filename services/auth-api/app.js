import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import helmet from 'helmet';
import session from 'express-session';
import passport from 'passport';
import path from 'path';
import { fileURLToPath } from 'url';
import setupPassport from './config/passport.js';
import './models/index.js';
import User from './models/User.js';
import Reclamacao from './models/Reclamacao.js';
import authRoutes from './routes/auth.js';
import usersRoutes from './routes/users.js';
import invitesRoutes from './routes/invites.js';
import userManagementRoutes from './routes/user-management.js';
import reclamacoesRoutes from './routes/reclamacoes.js';
import condominiosRoutes from './routes/condominios.js';
import perfisRoutes from './routes/perfis.js';
import portariaRoutes from './routes/portaria.js';
import estatisticasRoutes from './routes/estatisticas.js';
import { ehProducao } from './config/regras.js';

dotenv.config();

const __dirname = path.dirname(fileURLToPath(import.meta.url));

Reclamacao.belongsTo(User, { foreignKey: 'userId', as: 'usuario' });
User.hasMany(Reclamacao, { foreignKey: 'userId', as: 'reclamacoes' });

/**
 * Checagens de ambiente que derrubam o processo.
 *
 * Ficam numa função, e não no corpo do módulo, porque este arquivo é importado
 * pelos testes: um `process.exit(1)` durante a importação mataria o runner sem
 * mensagem. Quem sobe o serviço de verdade é o `server.js`, e é ele que chama
 * isto antes de qualquer outra coisa.
 */
export function validarAmbiente() {
  if (!process.env.JWT_SECRET) {
    console.error('ERRO: JWT_SECRET não definido no .env');
    process.exit(1);
  }

  // A sessão assina o `state` do OAuth. Reutilizar o JWT_SECRET faria as duas
  // chaves caírem juntas se uma vazasse.
  if (!process.env.SESSION_SECRET) {
    if (ehProducao()) {
      console.error('ERRO: SESSION_SECRET não definido no .env');
      process.exit(1);
    }
    console.warn('SESSION_SECRET não definido — usando fallback (apenas desenvolvimento)');
  }
}

setupPassport();

const app = express();

app.use(helmet({ crossOriginResourcePolicy: { policy: 'cross-origin' } }));
app.use(cors({ origin: process.env.FRONTEND_URL || 'http://localhost:5173', credentials: true }));
app.use(express.json({ limit: '100kb' }));

// A sessão existe só para guardar o `state` do OAuth entre o redirect e o
// callback — daí o cookie curto. A autenticação em si é via JWT.
app.use(session({
  name: 'mora.oauth',
  secret: process.env.SESSION_SECRET || process.env.JWT_SECRET,
  resave: false,
  saveUninitialized: false,
  cookie: {
    httpOnly: true,
    sameSite: 'lax',
    secure: ehProducao(),
    maxAge: 5 * 60 * 1000,
  },
}));
app.use(passport.initialize());

const uploadDir = process.env.STORAGE_PATH || path.join(__dirname, 'uploads', 'avatars');
app.use('/uploads/avatars', express.static(uploadDir));

app.use('/api/auth', authRoutes);
app.use('/api/invites', invitesRoutes);
app.use('/api/user-management', userManagementRoutes);
app.use('/api/users', usersRoutes);
app.use('/api/reclamacoes', reclamacoesRoutes);
app.use('/api/condominios', condominiosRoutes);
app.use('/api/perfis', perfisRoutes);
app.use('/api/portaria', portariaRoutes);
app.use('/api/estatisticas', estatisticasRoutes);

app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', message: 'Auth API funcionando' });
});

export default app;
