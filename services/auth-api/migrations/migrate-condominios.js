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

  // Inserir condomínios a partir dos condominioId distintos já existentes nos usuários
  await sequelize.query(`
    INSERT INTO condominios (id, nome, status, "createdAt", "updatedAt")
    SELECT DISTINCT
      "condominioId",
      CASE
        WHEN "condominioId" = 'default' THEN 'Condomínio Padrão'
        ELSE CONCAT('Condomínio ', "condominioId")
      END,
      'active',
      NOW(),
      NOW()
    FROM users
    WHERE "condominioId" IS NOT NULL
      AND "condominioId" NOT IN (SELECT id FROM condominios)
    ON CONFLICT (id) DO NOTHING
  `);

  // Garantir que o condomínio padrão exista mesmo sem usuários
  await sequelize.query(`
    INSERT INTO condominios (id, nome, status, "createdAt", "updatedAt")
    VALUES ('default', 'Condomínio Padrão', 'active', NOW(), NOW())
    ON CONFLICT (id) DO NOTHING
  `);

  // Adicionar ENUM status ao tipo se ainda não existir (PostgreSQL)
  await sequelize.query(`
    DO $$
    BEGIN
      IF NOT EXISTS (
        SELECT 1 FROM pg_type WHERE typname = 'enum_condominios_status'
      ) THEN
        CREATE TYPE "enum_condominios_status" AS ENUM ('active', 'inactive');
      END IF;
    END$$;
  `).catch(() => {});
}
