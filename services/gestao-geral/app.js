import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import dashboardRoutes from './routes/dashboard.js';

/**
 * A aplicação, sem `listen()` — os testes montam as rotas sem abrir porta nem
 * registrar no Consul. Mesmo desenho do auth-api e do financeiro.
 */
export const app = express();

app.use(helmet());
app.use(cors({ origin: process.env.FRONTEND_URL || 'http://localhost:5173', credentials: true }));
app.use(express.json({ limit: '32kb' }));

app.use('/api/gestao', dashboardRoutes);

app.get('/health', (_req, res) => {
  res.json({ status: 'ok', servico: 'gestao-geral' });
});

// Sem isto, um erro inesperado caía no tratador padrão do Express, que em
// desenvolvimento devolve o stack trace inteiro em HTML para quem chamou.
app.use((err, _req, res, _next) => {
  console.error('[gestao-geral] erro não tratado:', err);
  res.status(500).json({ sucesso: false, mensagem: 'Erro interno.' });
});
