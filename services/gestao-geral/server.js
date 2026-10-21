// Primeiro: em ESM os imports são avaliados antes do corpo do arquivo, então um
// `dotenv.config()` mais abaixo rodaria depois de os módulos já terem lido o env.
import 'dotenv/config';

import { app } from './app.js';
import { PORT, SERVICOS, ehProducao } from './config/servicos.js';
import { registrarNoConsul } from './config/consul.js';

// O segredo é compartilhado com o auth-api para validar os tokens que ele emite.
if (!process.env.JWT_SECRET) {
  console.error('ERRO: JWT_SECRET não definido no .env');
  process.exit(1);
}

const server = app.listen(PORT, async () => {
  console.log(`gestao-geral rodando em http://localhost:${PORT}`);
  console.log(`  auth-api:    ${SERVICOS.auth}`);
  console.log(`  portaria:    ${SERVICOS.portaria}`);
  console.log(`  plan:        ${SERVICOS.plan}`);
  console.log(`  comunicacao: ${SERVICOS.comunicacao}`);
  console.log(`  financeiro:  ${SERVICOS.financeiro}`);
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
