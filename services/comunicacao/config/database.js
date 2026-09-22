import pg from 'pg';

const { Pool, types } = pg;

// O driver entrega BIGINT como string para não perder precisão acima de 2^53.
// Aqui os BIGINT são contadores e ids sequenciais, que cabem folgado em Number.
// OID 20 = int8.
types.setTypeParser(20, (valor) => (valor === null ? null : Number(valor)));

export const pool = new Pool({
  host: process.env.POSTGRES_HOST || 'localhost',
  port: Number(process.env.POSTGRES_PORT || 5432),
  database: process.env.POSTGRES_DB || 'mora_comunicacao',
  user: process.env.POSTGRES_USER || 'admin',
  password: process.env.POSTGRES_PASSWORD,
  max: Number(process.env.POSTGRES_POOL_MAX || 10),
  idleTimeoutMillis: 30000,
});

pool.on('error', (err) => {
  // Conexão ociosa derrubada pelo servidor não deve matar o processo.
  console.error('[comunicacao] erro no pool do Postgres:', err.message);
});

/** Atalho para consultas simples. */
export const consultar = (texto, parametros) => pool.query(texto, parametros);

/**
 * Executa `fn` dentro de uma transação, com COMMIT no fim e ROLLBACK em erro.
 *
 * A mensagem depende disso: gravar a mensagem, mexer no carimbo da conversa e
 * notificar os outros participantes precisam cair ou valer juntos. Metade feito
 * deixaria conversa com aviso de mensagem que não existe, ou o contrário.
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
