import { fileURLToPath } from 'node:url';
import { runner } from 'node-pg-migrate';
import { pool } from './database.js';

const PASTA = fileURLToPath(new URL('../migrations', import.meta.url));

/**
 * Aplica as migrações pendentes de `migrations/` antes de o serviço atender.
 *
 * As migrações já eram versionadas com o node-pg-migrate, mas só rodavam por
 * `npm run migrate:up`, à mão. No Docker ninguém rodava: o container subia
 * contra um banco sem tabelas, ou sem a migração mais nova, e o erro aparecia
 * só na primeira requisição. Agora quem cria o banco é a própria subida.
 *
 * Usa uma conexão do pool, então vale a mesma configuração do resto do
 * serviço. O node-pg-migrate segura um advisory lock enquanto roda: duas
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
      // Só os avisos: o server.js imprime uma linha com o que foi aplicado.
      log: () => {},
    });
    return aplicadas.map((m) => m.name);
  } finally {
    cliente.release();
  }
}
