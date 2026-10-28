import { fileURLToPath } from 'node:url';
import { runner } from 'node-pg-migrate';
import { pool } from './database.js';

const PASTA = fileURLToPath(new URL('../migrations', import.meta.url));

/**
 * Aplica as migrações pendentes de `migrations/` na subida — mesmo desenho do
 * financeiro. O node-pg-migrate segura um advisory lock enquanto roda: duas
 * instâncias subindo juntas não aplicam a mesma migração duas vezes.
 */
export async function aplicarMigracoes() {
  const cliente = await pool.connect();
  try {
    const aplicadas = await runner({
      dbClient: cliente,
      dir: PASTA,
      direction: 'up',
      migrationsTable: 'pgmigrations',
      log: () => {},
    });
    return aplicadas.map((m) => m.name);
  } finally {
    cliente.release();
  }
}
