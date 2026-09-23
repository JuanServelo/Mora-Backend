import pg from 'pg';

const { Pool, types } = pg;

// O driver entrega BIGINT como string para não perder precisão acima de 2^53.
// Aqui todo valor monetário é BIGINT em centavos e cabe folgado em Number, então
// converter na borda do driver evita espalhar Number(...) por todo o código.
// OID 20 = int8.
types.setTypeParser(20, (valor) => (valor === null ? null : Number(valor)));

export const pool = new Pool({
  host: process.env.POSTGRES_HOST || 'localhost',
  port: Number(process.env.POSTGRES_PORT || 5432),
  database: process.env.POSTGRES_DB || 'mora_financeiro',
  user: process.env.POSTGRES_USER || 'admin',
  password: process.env.POSTGRES_PASSWORD,
  max: Number(process.env.POSTGRES_POOL_MAX || 10),
  idleTimeoutMillis: 30000,
});

pool.on('error', (err) => {
  // Conexão ociosa derrubada pelo servidor não deve matar o processo.
  console.error('[financeiro] erro no pool do Postgres:', err.message);
});

/** Atalho para consultas simples. */
export const consultar = (texto, parametros) => pool.query(texto, parametros);

/**
 * Executa `fn` dentro de uma transação, com COMMIT no fim e ROLLBACK em erro.
 *
 * O webhook depende disso: gravar o evento e dar baixa na fatura precisam ser
 * atômicos, senão uma reentrega dá baixa duas vezes.
 */
export async function emTransacao(fn) {
  const cliente = await pool.connect();
  try {
    await cliente.query('BEGIN');
    const resultado = await fn(cliente);
    await cliente.query('COMMIT');
    return resultado;
  } catch (err) {
    await cliente.query('ROLLBACK');
    throw err;
  } finally {
    cliente.release();
  }
}

export async function verificarConexao() {
  const { rows } = await pool.query('SELECT 1 AS ok');
  return rows[0]?.ok === 1;
}
