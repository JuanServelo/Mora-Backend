import { pool } from '../config/database.js';

const CAMPOS = `e.id, e.condominio_id AS "condominioId", e.fatura_id AS "faturaId",
  e.pagamento_plataforma_id AS "pagamentoPlataformaId", e.asaas_pagamento_id AS "asaasPagamentoId",
  e.valor_centavos AS "valorCentavos", e.motivo, e.status, e.detalhe,
  e.solicitado_por_id AS "solicitadoPorId", e.solicitado_em AS "solicitadoEm",
  e.concluido_em AS "concluidoEm"`;

// Todas recebem quem executa (`db`): o pool, ou o cliente de uma transação
// quando o estorno precisa andar junto com a fatura.

/**
 * Registra o pedido. Um segundo pedido para o mesmo pagamento esbarra no
 * `uq_estorno_por_pagamento` (23505), e quem chama responde 409.
 */
export async function criar(d, db = pool) {
  const { rows } = await db.query(
    `INSERT INTO estornos AS e
       (condominio_id, fatura_id, pagamento_plataforma_id, asaas_pagamento_id,
        valor_centavos, motivo, solicitado_por_id)
     VALUES ($1, $2, $3, $4, $5, $6, $7)
     RETURNING ${CAMPOS}`,
    [d.condominioId, d.faturaId ?? null, d.pagamentoPlataformaId ?? null, d.asaasPagamentoId,
      d.valorCentavos, d.motivo, d.solicitadoPorId ?? null],
  );
  return rows[0];
}

export async function porId(id, db = pool) {
  const { rows } = await db.query(`SELECT ${CAMPOS} FROM estornos e WHERE e.id = $1`, [id]);
  return rows[0] ?? null;
}

/** O pedido que vale para o pagamento: o em andamento ou o concluído. */
export async function vigentePorPagamento(asaasPagamentoId, db = pool) {
  const { rows } = await db.query(
    `SELECT ${CAMPOS} FROM estornos e
      WHERE e.asaas_pagamento_id = $1 AND e.status <> 'FALHOU'
      FOR UPDATE`,
    [asaasPagamentoId],
  );
  return rows[0] ?? null;
}

export async function marcarConcluido(id, db = pool) {
  await db.query(
    `UPDATE estornos SET status = 'CONCLUIDO', concluido_em = now() WHERE id = $1 AND status = 'SOLICITADO'`,
    [id],
  );
}

export async function marcarFalha(id, detalhe, db = pool) {
  await db.query(
    `UPDATE estornos SET status = 'FALHOU', detalhe = $2 WHERE id = $1 AND status = 'SOLICITADO'`,
    [id, detalhe ?? null],
  );
}

export async function pendentes(condominioId = null, db = pool) {
  const { rows } = await db.query(
    `SELECT ${CAMPOS} FROM estornos e
      WHERE e.status = 'SOLICITADO' AND ($1::varchar IS NULL OR e.condominio_id = $1)
      ORDER BY e.solicitado_em`,
    [condominioId],
  );
  return rows;
}

/** Estornos de faturas do condomínio, com a competência e a unidade da fatura. */
export async function listarDoCondominio(condominioId, db = pool) {
  const { rows } = await db.query(
    `SELECT ${CAMPOS}, f.competencia, f.unidade_id AS "unidadeId", f.status AS "faturaStatus"
       FROM estornos e
       JOIN faturas f ON f.id = e.fatura_id
      WHERE e.condominio_id = $1
      ORDER BY e.solicitado_em DESC`,
    [condominioId],
  );
  return rows;
}
