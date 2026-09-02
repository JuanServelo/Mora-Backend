import sequelize from '../config/database.js';

export async function garantirTabelaPortaria() {
  await sequelize.query(`
    ALTER TABLE users
      ADD COLUMN IF NOT EXISTS "entradaPermitida" BOOLEAN DEFAULT false
  `);

  await sequelize.query(`
    CREATE TABLE IF NOT EXISTS registros_acesso (
      id SERIAL PRIMARY KEY,
      "usuarioId"       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      tipo              VARCHAR(10) NOT NULL CHECK (tipo IN ('ENTRADA', 'SAIDA')),
      "registradoPorId" INTEGER REFERENCES users(id) ON DELETE SET NULL,
      "condominioId"    VARCHAR(50),
      "createdAt"       TIMESTAMP WITH TIME ZONE DEFAULT NOW()
    )
  `);

  await sequelize.query(`
    CREATE INDEX IF NOT EXISTS idx_registros_acesso_usuario
      ON registros_acesso ("usuarioId", "createdAt" DESC)
  `);
}
