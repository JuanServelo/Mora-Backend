// Primeiro import de todos, e nao por estilo: em ESM os modulos importados sao
// avaliados antes do corpo deste arquivo. Um `dotenv.config()` la embaixo
// rodaria depois de config/database.js ja ter lido process.env e montado o pool
// sem senha. Em Docker isso passa batido, porque o env vem do container.
import 'dotenv/config';

import { app } from './app.js';
import { PORT, SERVICOS, ehProducao } from './config/servicos.js';
import { registrarNoConsul } from './config/consul.js';
import { gatewayConfigurado } from './config/asaas.js';
import { iniciarJobOverdue } from './jobs/overdueFaturasJob.js';
import { aplicarMigracoes } from './config/migracoes.js';

// O segredo é compartilhado com o auth-api para validar os tokens que ele emite.
if (!process.env.JWT_SECRET) {
  console.error('ERRO: JWT_SECRET não definido no .env');
  process.exit(1);
}

if (!process.env.POSTGRES_PASSWORD) {
  console.error('ERRO: POSTGRES_PASSWORD não definido no .env');
  process.exit(1);
}

// Antes de escutar: com o banco atrás do código, cada rota quebraria por
// conta própria, na primeira requisição. Falhar aqui é falhar com o motivo.
try {
  const aplicadas = await aplicarMigracoes();
  console.log(aplicadas.length
    ? `Migrações aplicadas: ${aplicadas.join(', ')}`
    : 'Banco em dia: nenhuma migração pendente');
} catch (err) {
  console.error('ERRO: as migrações do banco falharam:', err.message);
  process.exit(1);
}

const server = app.listen(PORT, async () => {
  console.log(`financeiro rodando em http://localhost:${PORT}`);
  console.log(`  auth-api: ${SERVICOS.auth}`);
  console.log(`  banco:    ${process.env.POSTGRES_DB || 'mora_financeiro'}`);
  console.log(`  gateway:  ${gatewayConfigurado() ? 'configurado' : 'ASAAS_API_KEY ausente'}`);
  if (!ehProducao()) console.log('  ambiente: desenvolvimento');
  iniciarJobOverdue();
  await registrarNoConsul();
});

server.on('error', (err) => {
  if (err.code === 'EADDRINUSE') {
    console.error(`Porta ${PORT} em uso.`);
    process.exit(1);
  }
  throw err;
});
