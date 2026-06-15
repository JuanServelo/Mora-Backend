import sequelize from '../config/database.js';

/** Vincula usuários contratantes ao tenant (RF02). */
export async function garantirColunaTenantIdUsuarios() {
  await sequelize.query(`
    ALTER TABLE users
      ADD COLUMN IF NOT EXISTS tenant_id INTEGER REFERENCES tenants(id) ON DELETE SET NULL
  `);
}
