import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import dotenv from 'dotenv';
import dashboardRoutes from './routes/dashboard.js';
import { PORT, SERVICOS, ehProducao } from './config/servicos.js';
import { registrarNoConsul } from './config/consul.js';

dotenv.config();

// O segredo é compartilhado com o auth-api para validar os tokens que ele emite.
if (!process.env.JWT_SECRET) {
  console.error('ERRO: JWT_SECRET não definido no .env');
  process.exit(1);
}

const app = express();

app.use(helmet());
app.use(cors({ origin: process.env.FRONTEND_URL || 'http://localhost:5173', credentials: true }));
app.use(express.json({ limit: '32kb' }));

app.use('/api/gestao', dashboardRoutes);

app.get('/health', (_req, res) => {
  res.json({ status: 'ok', servico: 'gestao-geral' });
});

const server = app.listen(PORT, async () => {
  console.log(`gestao-geral rodando em http://localhost:${PORT}`);
  console.log(`  auth-api: ${SERVICOS.auth}`);
  console.log(`  portaria: ${SERVICOS.portaria}`);
  console.log(`  plan:     ${SERVICOS.plan}`);
  if (!ehProducao()) console.log('  ambiente: desenvolvimento');
  await registrarNoConsul();
});

server.on('error', (err) => {
  if (err.code === 'EADDRINUSE') {
    console.error(`Porta ${PORT} em uso.`);
    process.exit(1);
  }
  throw err;
});
