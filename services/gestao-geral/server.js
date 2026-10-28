// Primeiro: em ESM os imports são avaliados antes do corpo do arquivo, então um
// `dotenv.config()` mais abaixo rodaria depois de os módulos já terem lido o env.
import 'dotenv/config';

import { app } from './app.js';
import { PORT, SERVICOS, ehProducao } from './config/servicos.js';
import { registrarNoConsul } from './config/consul.js';
import { aplicarMigracoes } from './config/migracoes.js';

// O segredo é compartilhado com o auth-api para validar os tokens que ele emite.
if (!process.env.JWT_SECRET) {
  console.error('ERRO: JWT_SECRET não definido no .env');
  process.exit(1);
}

// O banco `mora_gestao` só guarda as preferências dos painéis. Se ele falhar,
// os painéis continuam no ar (no padrão de cada tela) e só a personalização
// fica indisponível — por isso aqui avisa e segue, em vez de derrubar o serviço.
if (process.env.POSTGRES_PASSWORD) {
  try {
    const aplicadas = await aplicarMigracoes();
    console.log(aplicadas.length
      ? `Migrações aplicadas: ${aplicadas.join(', ')}`
      : 'Banco em dia: nenhuma migração pendente');
  } catch (err) {
    console.error('AVISO: as migrações do mora_gestao falharam — preferências indisponíveis:', err.message);
  }
} else {
  console.warn('AVISO: POSTGRES_PASSWORD não definido — preferências dos painéis indisponíveis.');
}

const server = app.listen(PORT, async () => {
  console.log(`gestao-geral rodando em http://localhost:${PORT}`);
  console.log(`  auth-api:    ${SERVICOS.auth}`);
  console.log(`  portaria:    ${SERVICOS.portaria}`);
  console.log(`  plan:        ${SERVICOS.plan}`);
  console.log(`  comunicacao: ${SERVICOS.comunicacao}`);
  console.log(`  financeiro:  ${SERVICOS.financeiro}`);
  console.log(`  ocorrencias: ${SERVICOS.ocorrencias}`);
  console.log(`  banco:       ${process.env.POSTGRES_DB || 'mora_gestao'}`);
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
