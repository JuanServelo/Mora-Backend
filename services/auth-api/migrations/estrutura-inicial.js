import sequelize from '../config/database.js';

/**
 * Tabelas de que todas as outras migrações dependem.
 *
 * Até aqui elas nasciam do `docker/init-databases.sql`, que o Postgres só roda
 * quando o volume é criado do zero, e as migrações seguintes pressupunham que
 * ele tinha rodado. Agora o banco nasce inteiro pelas migrações, e o init
 * cuida só de criar os bancos.
 *
 * É o mesmo SQL de antes, de propósito: as migrações seguintes foram escritas
 * sobre esta estrutura. `users` ainda nasce com `role`, `bloco`, `apartamento`
 * e `vaga`, que `migrate-remover-legado` converte e derruba mais adiante.
 *
 * Fica de fora o administrador com senha fixa que o init inseria. A conta
 * inicial vem de `ADMIN_SEED_EMAIL`/`ADMIN_SEED_PASSWORD`, no `server.js`.
 *
 * Idempotente (IF NOT EXISTS): num banco que já existe, nada muda.
 */
export async function criarEstruturaInicial() {
  await sequelize.query(`
    CREATE TABLE IF NOT EXISTS condominios (
      id VARCHAR(50) PRIMARY KEY,
      nome VARCHAR(200) NOT NULL,
      cnpj VARCHAR(18),
      endereco VARCHAR(300),
      telefone VARCHAR(20),
      email VARCHAR(150),
      status VARCHAR(20) DEFAULT 'active',
      "criadoPorId" INTEGER,
      "createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      "updatedAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    )
  `);

  await sequelize.query(`
    INSERT INTO condominios (id, nome, status) VALUES ('default', 'Condomínio Padrão', 'active')
    ON CONFLICT (id) DO NOTHING
  `);

  await sequelize.query(`
    CREATE TABLE IF NOT EXISTS users (
      id SERIAL PRIMARY KEY,
      nome VARCHAR(255) NOT NULL,
      email VARCHAR(255) NOT NULL UNIQUE,
      senha VARCHAR(255),
      "googleId" VARCHAR(255) UNIQUE,
      provider VARCHAR(20) DEFAULT 'local' CHECK (provider IN ('local', 'google')),
      role VARCHAR(20) DEFAULT 'user' CHECK (role IN ('user', 'admin')),
      bloco VARCHAR(50),
      apartamento VARCHAR(50),
      vaga VARCHAR(50),
      "resetToken" VARCHAR(255),
      "resetTokenExpira" TIMESTAMP,
      telefone VARCHAR(20),
      cpf VARCHAR(14),
      "fotoUrl" VARCHAR(500),
      perfil VARCHAR(50),
      status VARCHAR(30) DEFAULT 'pending_activation',
      "condominioId" VARCHAR(50),
      "unidadeId" UUID,
      "cadastradoPorId" INTEGER,
      "responsavelFinanceiro" BOOLEAN DEFAULT false,
      "dataNascimento" DATE,
      "semAcessoSistema" BOOLEAN DEFAULT false,
      "entradaPermitida" BOOLEAN DEFAULT false,
      "oauthCode" VARCHAR(255),
      "oauthCodeExpira" TIMESTAMP,
      "tokenVersion" INTEGER DEFAULT 0,
      "activatedAt" TIMESTAMP,
      "createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      "updatedAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    )
  `);

  await sequelize.query(`
    CREATE TABLE IF NOT EXISTS invites (
      id SERIAL PRIMARY KEY,
      codigo VARCHAR(12) NOT NULL UNIQUE,
      email VARCHAR(150) NOT NULL,
      perfil VARCHAR(50) NOT NULL,
      "condominioId" VARCHAR(50) NOT NULL,
      "unidadeId" UUID,
      "nomePrecadastro" VARCHAR(150),
      "cpfPrecadastro" VARCHAR(14),
      "cadastradoPorId" INTEGER NOT NULL,
      status VARCHAR(20) DEFAULT 'pending',
      "expiresAt" TIMESTAMP NOT NULL,
      "usedAt" TIMESTAMP,
      "usedByUserId" INTEGER,
      "responsavelFinanceiro" BOOLEAN NOT NULL DEFAULT false,
      "createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      "updatedAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    )
  `);

  // Portaria: histórico de entradas e saídas, um registro por evento.
  await sequelize.query(`
    CREATE TABLE IF NOT EXISTS registros_acesso (
      id                SERIAL PRIMARY KEY,
      "usuarioId"       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      tipo              VARCHAR(10) NOT NULL CHECK (tipo IN ('ENTRADA', 'SAIDA')),
      "registradoPorId" INTEGER REFERENCES users(id) ON DELETE SET NULL,
      "condominioId"    VARCHAR(50),
      "nomeSnapshot"    VARCHAR(150),
      "perfilSnapshot"  VARCHAR(50),
      "createdAt"       TIMESTAMP WITH TIME ZONE DEFAULT NOW()
    )
  `);

  await sequelize.query(`
    CREATE INDEX IF NOT EXISTS idx_registros_acesso_usuario
      ON registros_acesso ("usuarioId", "createdAt" DESC)
  `);
  await sequelize.query(`
    CREATE INDEX IF NOT EXISTS idx_registros_acesso_condominio
      ON registros_acesso ("condominioId", "createdAt" DESC)
  `);
  await sequelize.query('CREATE INDEX IF NOT EXISTS idx_condominios_status ON condominios(status)');
  await sequelize.query('CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)');
  await sequelize.query('CREATE INDEX IF NOT EXISTS idx_users_googleId ON users("googleId")');
  await sequelize.query('CREATE INDEX IF NOT EXISTS idx_users_condominioId ON users("condominioId")');
  await sequelize.query('CREATE INDEX IF NOT EXISTS idx_users_unidadeId ON users("unidadeId")');
  await sequelize.query('CREATE INDEX IF NOT EXISTS idx_invites_condominioId ON invites("condominioId")');
}
