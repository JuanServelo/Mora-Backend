import path from 'node:path';
import { fileURLToPath } from 'node:url';
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import swaggerUiDist from 'swagger-ui-dist';
import dashboardRoutes from './routes/dashboard.js';
import preferenciasRoutes from './routes/preferencias.js';
import cambioRoutes from './routes/cambio.js';
import { openapi } from './docs/openapi.js';

const aqui = path.dirname(fileURLToPath(import.meta.url));

/**
 * A aplicação, sem `listen()` — os testes montam as rotas sem abrir porta nem
 * registrar no Consul. Mesmo desenho do auth-api e do financeiro.
 */
export const app = express();

// Sem `upgrade-insecure-requests`: no ambiente local tudo é http, e a diretiva
// faria o navegador pedir os arquivos do portal /docs por https.
app.use(helmet({ contentSecurityPolicy: { directives: { upgradeInsecureRequests: null } } }));
app.use(cors({ origin: process.env.FRONTEND_URL || 'http://localhost:5173', credentials: true }));
app.use(express.json({ limit: '32kb' }));

// Documentação: antes do router de /api/gestao, que exige token em tudo.
// O portal reúne a especificação de todos os serviços; esta é a deste.
app.get('/api/gestao/docs/openapi.json', (_req, res) => res.json(openapi));
app.use('/docs/swagger-ui', express.static(swaggerUiDist.getAbsoluteFSPath()));
app.get('/docs', (_req, res) => res.sendFile(path.join(aqui, 'docs/portal/index.html')));
app.use('/docs', express.static(path.join(aqui, 'docs/portal')));

// Antes do dashboard: o router dele exige token em tudo que passa por ele, e
// a recusa das preferências tem regra própria (chave e sessão).
app.use('/api/gestao/preferencias', preferenciasRoutes);
// Também antes: o dashboard exige perfil de gestão, e a cotação é para todos.
app.use('/api/gestao/cambio', cambioRoutes);
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
