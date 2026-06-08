import sequelize from '../config/database.js';
import { PERFIS, STATUS_USUARIO } from '../constants/perfis.js';

export async function garantirColunasRf07() {
  await sequelize.query(`
    ALTER TABLE users
      ADD COLUMN IF NOT EXISTS "dataNascimento" DATE,
      ADD COLUMN IF NOT EXISTS "semAcessoSistema" BOOLEAN DEFAULT false
  `);

  await sequelize.query(`
    ALTER TABLE users ALTER COLUMN email DROP NOT NULL
  `).catch(() => {});

  await sequelize.query(`
    UPDATE users
    SET "responsavelFinanceiro" = true
    WHERE perfil = '${PERFIS.RESIDENT_OWNER}'
      AND status = '${STATUS_USUARIO.ACTIVE}'
      AND "unidadeId" IS NOT NULL
      AND "responsavelFinanceiro" = false
  `);
}
