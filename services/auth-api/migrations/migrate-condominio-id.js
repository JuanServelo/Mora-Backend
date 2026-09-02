import sequelize from '../config/database.js';
import { CONDOMINIO_DEFAULT } from '../constants/perfis.js';

/**
 * Associa as ocorrências ao condomínio dono do registro.
 *
 * O condomínio é o cliente da plataforma: todo dado de domínio precisa apontar
 * para ele, senão um síndico enxerga a operação de outro condomínio.
 * `reclamacoes` era a última tabela do auth_db sem esse vínculo.
 */
export async function garantirCondominioIdReclamacoes() {
  await sequelize.query(`
    ALTER TABLE reclamacoes
      ADD COLUMN IF NOT EXISTS "condominioId" VARCHAR(50)
  `);

  // Backfill pelo autor: a ocorrência pertence ao condomínio de quem a abriu.
  const [, meta] = await sequelize.query(`
    UPDATE reclamacoes r
    SET "condominioId" = u."condominioId"
    FROM users u
    WHERE r."userId" = u.id
      AND r."condominioId" IS NULL
      AND u."condominioId" IS NOT NULL
  `);

  // Sobras (autor sem condomínio) vão para o condomínio padrão.
  await sequelize.query(`
    UPDATE reclamacoes
    SET "condominioId" = '${CONDOMINIO_DEFAULT}'
    WHERE "condominioId" IS NULL
  `);

  await sequelize.query(`
    CREATE INDEX IF NOT EXISTS idx_reclamacoes_condominioId
      ON reclamacoes("condominioId")
  `);

  if (meta?.rowCount) {
    console.log(`Ocorrências vinculadas ao condomínio: ${meta.rowCount}`);
  }
}
