import { consultar } from '../config/database.js';

export async function listarPorCondominio(condominioId) {
  const { rows } = await consultar(
    `SELECT unidade_id AS "unidadeId", fracao_milesimos AS "milesimos"
       FROM unidades_fracao
      WHERE condominio_id = $1`,
    [condominioId],
  );
  return rows;
}

export async function resumo(condominioId) {
  const { rows } = await consultar(
    `SELECT COALESCE(SUM(fracao_milesimos), 0)::int AS soma, COUNT(*)::int AS total
       FROM unidades_fracao
      WHERE condominio_id = $1`,
    [condominioId],
  );
  return rows[0];
}

/** Grava a fração de uma unidade, substituindo a anterior se houver. */
export async function definir(condominioId, unidadeId, milesimos) {
  const { rows } = await consultar(
    `INSERT INTO unidades_fracao (condominio_id, unidade_id, fracao_milesimos)
          VALUES ($1, $2, $3)
     ON CONFLICT (condominio_id, unidade_id)
       DO UPDATE SET fracao_milesimos = EXCLUDED.fracao_milesimos,
                     atualizado_em = now()
       RETURNING unidade_id AS "unidadeId", fracao_milesimos AS "milesimos"`,
    [condominioId, unidadeId, milesimos],
  );
  return rows[0];
}

export async function remover(condominioId, unidadeId) {
  const { rowCount } = await consultar(
    'DELETE FROM unidades_fracao WHERE condominio_id = $1 AND unidade_id = $2',
    [condominioId, unidadeId],
  );
  return rowCount > 0;
}

/** Grava várias numa transação só — a importação é tudo ou nada. */
export async function definirEmLote(cliente, condominioId, entradas) {
  for (const { unidadeId, milesimos } of entradas) {
    await cliente.query(
      `INSERT INTO unidades_fracao (condominio_id, unidade_id, fracao_milesimos)
            VALUES ($1, $2, $3)
       ON CONFLICT (condominio_id, unidade_id)
         DO UPDATE SET fracao_milesimos = EXCLUDED.fracao_milesimos,
                       atualizado_em = now()`,
      [condominioId, unidadeId, milesimos],
    );
  }
  return entradas.length;
}
