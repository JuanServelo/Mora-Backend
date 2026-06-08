import sequelize from '../config/database.js';

export async function garantirTabelaCondominios() {
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

  // Criar ENUM type antes dos INSERTs
  await sequelize.query(`
    DO $$
    BEGIN
      IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'enum_condominios_status') THEN
        CREATE TYPE "enum_condominios_status" AS ENUM ('active', 'inactive');
      END IF;
    END$$;
  `);

  // Converter coluna status de VARCHAR para ENUM (deve rodar antes dos INSERTs)
  await sequelize.query(`
    DO $$
    BEGIN
      IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'condominios'
          AND column_name = 'status'
          AND data_type = 'character varying'
      ) THEN
        ALTER TABLE condominios ALTER COLUMN status DROP DEFAULT;
        ALTER TABLE condominios ALTER COLUMN status TYPE "enum_condominios_status"
          USING status::"enum_condominios_status";
        ALTER TABLE condominios ALTER COLUMN status SET DEFAULT 'active'::"enum_condominios_status";
      END IF;
    END$$;
  `);

  // INSERTs com cast explícito para ENUM
  await sequelize.query(`
    INSERT INTO condominios (id, nome, status, "createdAt", "updatedAt")
    SELECT DISTINCT
      "condominioId",
      CASE
        WHEN "condominioId" = 'default' THEN 'Condomínio Padrão'
        ELSE CONCAT('Condomínio ', "condominioId")
      END,
      'active'::"enum_condominios_status",
      NOW(),
      NOW()
    FROM users
    WHERE "condominioId" IS NOT NULL
      AND "condominioId" NOT IN (SELECT id FROM condominios)
    ON CONFLICT (id) DO NOTHING
  `);

  await sequelize.query(`
    INSERT INTO condominios (id, nome, status, "createdAt", "updatedAt")
    VALUES ('default', 'Condomínio Padrão', 'active'::"enum_condominios_status", NOW(), NOW())
    ON CONFLICT (id) DO NOTHING
  `);

  // Criar tabela reclamacoes se não existir (não está no init-databases.sql)
  await sequelize.query(`
    CREATE TABLE IF NOT EXISTS reclamacoes (
      id SERIAL PRIMARY KEY,
      "userId" INTEGER NOT NULL REFERENCES users(id),
      "protocolNumber" VARCHAR(255) NOT NULL UNIQUE,
      category VARCHAR(255) NOT NULL,
      description TEXT NOT NULL,
      "attachmentUrl" VARCHAR(255),
      status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
      interactions JSON NOT NULL DEFAULT '[]',
      "createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      "updatedAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    )
  `);
}
