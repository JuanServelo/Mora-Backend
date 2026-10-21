import sequelize from './config/database.js';
import User from './models/User.js';
import app, { validarAmbiente } from './app.js';
import { executarMigracoes } from './migrations/index.js';
import { PERFIS, STATUS_USUARIO } from './constants/perfis.js';
import { registrarNoConsul } from './config/consul.js';

validarAmbiente();

const PORT = process.env.PORT || 3001;

/**
 * Cria a conta administrativa inicial a partir do .env. A senha nunca fica no
 * código: sem ADMIN_SEED_PASSWORD definido, o seed é pulado — em produção isso
 * é obrigatório, e em desenvolvimento apenas avisa.
 */
const seedAdminUser = async () => {
  const existing = await User.findOne({ where: { perfil: PERFIS.ADMIN_GERAL } });
  if (existing) return;

  const adminEmail = process.env.ADMIN_SEED_EMAIL;
  const adminSenha = process.env.ADMIN_SEED_PASSWORD;

  if (!adminEmail || !adminSenha) {
    console.warn(
      'ADMIN_SEED_EMAIL/ADMIN_SEED_PASSWORD não definidos — seed do admin pulado. '
      + 'Defina-os no .env para criar a conta administrativa inicial.',
    );
    return;
  }

  await User.create({
    nome: process.env.ADMIN_SEED_NOME || 'Admin',
    email: adminEmail,
    senha: adminSenha,
    perfil: PERFIS.ADMIN_GERAL,
    status: STATUS_USUARIO.ACTIVE,
    activatedAt: new Date(),
  });
  console.log(`Usuário admin criado: ${adminEmail}`);
};

const startServer = async () => {
  try {
    await sequelize.authenticate();
    console.log('PostgreSQL conectado');
    // Ordem e histórico em migrations/index.js.
    const aplicadas = await executarMigracoes();
    console.log(aplicadas.length
      ? `Migrações aplicadas: ${aplicadas.join(', ')}`
      : 'Banco em dia: nenhuma migração pendente');
    await seedAdminUser();
  } catch (err) {
    // O serviço sobe mesmo assim, como antes: as migrações que faltaram
    // continuam pendentes e rodam na próxima subida.
    console.error('Erro ao migrar o banco:', err.message);
  }

  const server = app.listen(PORT, () => {
    console.log(`Servidor rodando em http://localhost:${PORT}`);
    // Só depois de escutar: o Consul checa /api/health logo em seguida.
    registrarNoConsul(PORT);
  });

  server.on('error', (err) => {
    if (err.code === 'EADDRINUSE') {
      console.error(`Porta ${PORT} em uso.`);
      process.exit(1);
    }
    throw err;
  });
};

startServer();
