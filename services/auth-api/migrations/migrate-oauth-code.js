import sequelize from '../config/database.js';

/**
 * Colunas do código de troca do OAuth. Substituem o envio do JWT na query
 * string do redirect do Google, que vazava o token para histórico do
 * navegador, logs de proxy e cabeçalho Referer.
 */
export async function garantirColunasOauthCode() {
  await sequelize.query(`
    ALTER TABLE users
      ADD COLUMN IF NOT EXISTS "oauthCode" VARCHAR(255),
      ADD COLUMN IF NOT EXISTS "oauthCodeExpira" TIMESTAMP
  `);

  // O código é consultado pelo hash na troca; sem índice seria full scan.
  await sequelize.query(`
    CREATE INDEX IF NOT EXISTS idx_users_oauthCode ON users("oauthCode")
  `);
}
