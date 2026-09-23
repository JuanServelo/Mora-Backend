import sequelize from '../config/database.js';

/**
 * Snapshot do turno no registro de acesso (RF-09).
 *
 * Gravar o turno previsto no momento da entrada é o que impede que alterar a
 * escala depois reescreva retroativamente o que era "dentro do turno" em todo
 * o histórico — mesmo princípio já aplicado a nome e perfil.
 */
export async function garantirColunasJornadaSnapshot() {
  await sequelize.query(`
    ALTER TABLE registros_acesso
      ADD COLUMN IF NOT EXISTS "turnoPrevisto" VARCHAR(50),
      ADD COLUMN IF NOT EXISTS "liberacaoExcepcional" BOOLEAN NOT NULL DEFAULT false
  `);
}
