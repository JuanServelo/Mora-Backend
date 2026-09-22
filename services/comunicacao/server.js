// Primeiro import de todos, e nao por estilo: em ESM os modulos importados sao
// avaliados antes do corpo deste arquivo. Um `dotenv.config()` la embaixo
// rodaria depois de config/database.js ja ter lido process.env e montado o pool
// sem senha. Em Docker isso passa batido, porque o env vem do container.
import 'dotenv/config';

import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import { DIRETORIO_AVISOS } from './config/upload.js';
import notificacoesRoutes from './routes/notificacoes.js';
import conversasRoutes from './routes/conversas.js';
import leiturasRoutes from './routes/leituras.js';
import internoRoutes from './routes/interno.js';
import { PORT, SERVICOS, TOKEN_SERVICO, ehProducao } from './config/servicos.js';
import { registrarNoConsul } from './config/consul.js';
import { verificarConexao } from './config/database.js';

// O segredo é compartilhado com o auth-api para validar os tokens que ele emite.
if (!process.env.JWT_SECRET) {
  console.error('ERRO: JWT_SECRET não definido no .env');
  process.exit(1);
}

if (!process.env.POSTGRES_PASSWORD) {
  console.error('ERRO: POSTGRES_PASSWORD não definido no .env');
  process.exit(1);
}

const app = express();

app.use(helmet());
app.use(cors({ origin: process.env.FRONTEND_URL || 'http://localhost:5173', credentials: true }));
app.use(express.json({ limit: '64kb' }));

// As imagens dos avisos. `crossOriginResourcePolicy` do helmet bloqueia por
// padrão o carregamento de recurso por outra origem, e a tela roda em :5173.
app.use(
  '/uploads/avisos',
  helmet.crossOriginResourcePolicy({ policy: 'cross-origin' }),
  express.static(DIRETORIO_AVISOS, { maxAge: '7d' }),
);

// A rota interna tem prefixo próprio, e não é organização: um
// `router.use(autenticar)` roda para toda requisição que ENTRA no router, e não
// só para as que casam com alguma rota dele. Montada no mesmo prefixo dos
// demais, ela levava 401 do middleware de JWT de outro router antes de chegar
// ao seu — e ela se autentica por credencial de serviço, não por token de
// usuário. Prefixo separado, e primeiro na ordem, resolve dos dois lados.
app.use('/api/comunicacao/interno', internoRoutes);

app.use('/api/comunicacao', notificacoesRoutes);
app.use('/api/comunicacao', conversasRoutes);
app.use('/api/comunicacao', leiturasRoutes);

app.get('/health', async (_req, res) => {
  const banco = await verificarConexao().catch(() => false);
  res.status(banco ? 200 : 503).json({
    status: banco ? 'ok' : 'degradado',
    servico: 'comunicacao',
    banco: banco ? 'ok' : 'indisponivel',
    publicacaoInterna: TOKEN_SERVICO ? 'configurada' : 'ausente',
  });
});

app.use((err, _req, res, _next) => {
  console.error('[comunicacao] erro não tratado:', err);
  // Mensagem genérica de propósito: erro de banco costuma vazar nome de tabela
  // e trecho de SQL para quem chamou.
  res.status(500).json({ sucesso: false, mensagem: 'Erro interno.' });
});

const server = app.listen(PORT, async () => {
  console.log(`comunicacao rodando em http://localhost:${PORT}`);
  console.log(`  auth-api: ${SERVICOS.auth}`);
  console.log(`  portaria: ${SERVICOS.portaria}`);
  console.log(`  banco:    ${process.env.POSTGRES_DB || 'mora_comunicacao'}`);
  if (!TOKEN_SERVICO) {
    console.log('  interno:  SERVICO_TOKEN ausente — /interno responde 503');
  }
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
