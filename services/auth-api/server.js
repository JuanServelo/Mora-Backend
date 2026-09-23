import sequelize from './config/database.js';
import User from './models/User.js';
import app, { validarAmbiente } from './app.js';
import { garantirColunasNovas, migrarUsuariosLegados } from './migrations/migrate-rf03.js';
import { garantirColunasRf07 } from './migrations/migrate-rf07.js';
import { garantirTabelaCondominios } from './migrations/migrate-condominios.js';
import { garantirTabelaPortaria } from './migrations/migrate-portaria.js';
import { garantirColunasOauthCode } from './migrations/migrate-oauth-code.js';
import { garantirColunasJornadaSnapshot } from './migrations/migrate-jornada-snapshot.js';
import { migrarPerfisV2 } from './migrations/migrate-perfis-v2.js';
import { garantirCondominioIdReclamacoes } from './migrations/migrate-condominio-id.js';
import { removerColunasLegado } from './migrations/migrate-remover-legado.js';
import { garantirColunasInvites } from './migrations/migrate-invites.js';
import { migrarPerfilTerceiro } from './migrations/migrate-terceiro.js';
import { PERFIS, STATUS_USUARIO } from './constants/perfis.js';

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
    await garantirColunasNovas();
    await garantirColunasRf07();
    await garantirTabelaCondominios();
    await garantirColunasOauthCode();
    await garantirColunasJornadaSnapshot();
    await migrarUsuariosLegados();
    // Depois das legadas: converte os 11 perfis antigos para os 6 atuais.
    await migrarPerfisV2();
    await garantirCondominioIdReclamacoes();
    await garantirColunasInvites();
    await migrarPerfilTerceiro();
    // Por último: as anteriores ainda leem as colunas que esta remove.
    await removerColunasLegado();
    console.log('Tabelas sincronizadas e migrações RF03/RF07/Condomínios aplicadas');
    await seedAdminUser();
  } catch (err) {
    console.error('Erro ao sincronizar tabelas:', err.message);
  }

  // Migração isolada: tabela registros_acesso e colunas de portaria.
  // Bloco separado para garantir execução mesmo se outras migrações falharem.
  try {
    await garantirTabelaPortaria();
  } catch (err) {
    console.error('Erro ao migrar tabela portaria:', err.message);
  }

  const server = app.listen(PORT, () => {
    console.log(`Servidor rodando em http://localhost:${PORT}`);
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
