import sequelize from '../config/database.js';
import { STATUS_USUARIO, CONDOMINIO_DEFAULT } from '../constants/perfis.js';
import { usaEnumPerfilLegado } from './perfilEnumVersao.js';

// Valores anteriores a migrate-perfis-v2: esta migracao roda sobre o enum
// antigo e por isso usa os rotulos legados como literais.
const LEGADO_ADMIN = 'ADMINISTRATOR';
const LEGADO_MORADOR = 'RESIDENT_OWNER';

function roleParaPerfil(role) {
  if (role === 'admin') return LEGADO_ADMIN;
  return LEGADO_MORADOR;
}

/**
 * Migra usuários legados: role → perfil, status → active.
 */
export async function migrarUsuariosLegados() {
  // Escreve rótulos de perfil antigos; após migrate-perfis-v2 não se aplica.
  if (!(await usaEnumPerfilLegado())) return;

  const [rows] = await sequelize.query(`
    SELECT id, role, perfil, status FROM users
  `);

  for (const row of rows) {
    const updates = [];
    const replacements = { id: row.id };

    if (!row.perfil) {
      updates.push('perfil = :perfil');
      replacements.perfil = roleParaPerfil(row.role);
    }
    if (!row.status) {
      updates.push('status = :status');
      replacements.status = STATUS_USUARIO.ACTIVE;
    }
    if (!row.condominioId) {
      updates.push('"condominioId" = :condominioId');
      replacements.condominioId = CONDOMINIO_DEFAULT;
    }

    if (updates.length > 0) {
      await sequelize.query(
        `UPDATE users SET ${updates.join(', ')} WHERE id = :id`,
        { replacements },
      );
    }
  }

  // Usuários admin legados → ADMINISTRATOR ativo
  await sequelize.query(`
    UPDATE users
    SET perfil = '${LEGADO_ADMIN}',
        status = '${STATUS_USUARIO.ACTIVE}',
        "activatedAt" = COALESCE("activatedAt", "createdAt")
    WHERE role = 'admin' AND (perfil IS NULL OR perfil = '${LEGADO_MORADOR}')
  `);

  await sequelize.query(`
    UPDATE users
    SET status = '${STATUS_USUARIO.ACTIVE}',
        "activatedAt" = COALESCE("activatedAt", "createdAt")
    WHERE senha IS NOT NULL AND status = '${STATUS_USUARIO.PENDING_ACTIVATION}'
  `);
}

export async function garantirColunasNovas() {
  await sequelize.query(`
    ALTER TABLE users
      ADD COLUMN IF NOT EXISTS telefone VARCHAR(20),
      ADD COLUMN IF NOT EXISTS cpf VARCHAR(14),
      ADD COLUMN IF NOT EXISTS "fotoUrl" VARCHAR(500),
      ADD COLUMN IF NOT EXISTS perfil VARCHAR(50),
      ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'pending_activation',
      ADD COLUMN IF NOT EXISTS "condominioId" VARCHAR(50),
      ADD COLUMN IF NOT EXISTS "unidadeId" UUID,
      ADD COLUMN IF NOT EXISTS "cadastradoPorId" INTEGER,
      ADD COLUMN IF NOT EXISTS "responsavelFinanceiro" BOOLEAN DEFAULT false,
      ADD COLUMN IF NOT EXISTS "tokenVersion" INTEGER DEFAULT 0,
      ADD COLUMN IF NOT EXISTS "activatedAt" TIMESTAMP,
      ADD COLUMN IF NOT EXISTS bloco VARCHAR(50),
      ADD COLUMN IF NOT EXISTS apartamento VARCHAR(50),
      ADD COLUMN IF NOT EXISTS vaga VARCHAR(50)
  `);
}
