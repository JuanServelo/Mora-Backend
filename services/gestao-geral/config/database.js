import pg from 'pg';

/**
 * Banco próprio do gestao-geral, `mora_gestao`.
 *
 * Até aqui o serviço não tinha banco: só agregava o que os outros devolvem. O
 * que ele guarda é dele mesmo — como cada usuário monta os próprios painéis —
 * e não pertence a nenhum dos serviços que ele consulta.
 *
 * O pool não abre conexão ao ser criado, só na primeira consulta: os testes
 * importam o app sem precisar de banco.
 */
export const pool = new pg.Pool({
  host: process.env.POSTGRES_HOST || 'localhost',
  port: Number(process.env.POSTGRES_PORT || 5432),
  database: process.env.POSTGRES_DB || 'mora_gestao',
  user: process.env.POSTGRES_USER || 'admin',
  password: process.env.POSTGRES_PASSWORD,
  max: Number(process.env.POSTGRES_POOL_MAX || 5),
  idleTimeoutMillis: 30000,
});

pool.on('error', (err) => {
  // Conexão ociosa derrubada pelo servidor não deve matar o processo.
  console.error('[gestao-geral] erro no pool do Postgres:', err.message);
});

export const consultar = (texto, parametros) => pool.query(texto, parametros);
