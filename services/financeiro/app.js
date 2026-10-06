import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import contextoRoutes from './routes/contexto.js';
import cadastrosRoutes from './routes/cadastros.js';
import gatewayRoutes from './routes/gateway.js';
import contasConsumoRoutes from './routes/contasConsumo.js';
import faturasRoutes from './routes/faturas.js';
import webhooksRoutes from './routes/webhooks.js';
import multasRoutes from './routes/multas.js';
import contratosRoutes from './routes/contratos.js';
import prestacaoRoutes from './routes/prestacao.js';
import { verificarConexao } from './config/database.js';
import { gatewayConfigurado } from './config/asaas.js';

/**
 * A aplicação, sem `listen()`.
 *
 * Separada do `server.js` para os testes montarem as rotas sem abrir porta,
 * registrar no Consul nem ligar o job diário — mesmo desenho do auth-api.
 */
export const app = express();

app.use(helmet());
app.use(cors({ origin: process.env.FRONTEND_URL || 'http://localhost:5173', credentials: true }));

// O webhook do gateway precisa do corpo cru para conferir a assinatura, então
// entra antes do parser de JSON e guarda os bytes originais.
app.use('/api/financeiro/webhooks', express.json({
  limit: '256kb',
  verify: (req, _res, buf) => { req.corpoCru = buf; },
}));

app.use(express.json({ limit: '64kb' }));

app.use('/api/financeiro', contextoRoutes);
app.use('/api/financeiro', cadastrosRoutes);
app.use('/api/financeiro', gatewayRoutes);
app.use('/api/financeiro', contasConsumoRoutes);
app.use('/api/financeiro', faturasRoutes);
app.use('/api/financeiro', webhooksRoutes);
app.use('/api/financeiro', multasRoutes);
app.use('/api/financeiro', contratosRoutes);
app.use('/api/financeiro', prestacaoRoutes);

app.get('/health', async (_req, res) => {
  const banco = await verificarConexao().catch(() => false);
  res.status(banco ? 200 : 503).json({
    status: banco ? 'ok' : 'degradado',
    servico: 'financeiro',
    banco: banco ? 'ok' : 'indisponivel',
    gateway: gatewayConfigurado() ? 'configurado' : 'ausente',
  });
});

app.use((err, _req, res, _next) => {
  console.error('[financeiro] erro não tratado:', err);
  // Mensagem genérica de propósito: erro de banco costuma vazar nome de tabela
  // e trecho de SQL para quem chamou.
  res.status(500).json({ sucesso: false, mensagem: 'Erro interno.' });
});
