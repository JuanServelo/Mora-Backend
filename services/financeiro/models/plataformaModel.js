import { pool } from '../config/database.js';

const CAMPOS_ASSINATURA = `id, condominio_id AS "condominioId", plano_nome AS "planoNome",
  plano_assinatura_id AS "planoAssinaturaId", asaas_assinatura_id AS "asaasAssinaturaId",
  asaas_cliente_id AS "asaasClienteId", forma, valor_centavos AS "valorCentavos", ciclo, status,
  ativada_por_id AS "ativadaPorId", criado_em AS "criadoEm", cancelada_em AS "canceladaEm"`;

const CAMPOS_PAGAMENTO = `id, assinatura_id AS "assinaturaId", asaas_pagamento_id AS "asaasPagamentoId",
  valor_centavos AS "valorCentavos", to_char(vencimento, 'YYYY-MM-DD') AS vencimento, status, forma,
  url_pagamento AS "urlPagamento", to_char(pago_em, 'YYYY-MM-DD') AS "pagoEm", atualizado_em AS "atualizadoEm"`;

/**
 * Grava a assinatura. Uma segunda ATIVA para o mesmo condomínio esbarra no
 * `uq_assinatura_plataforma_ativa` (23505).
 */
export async function criarAssinatura(d, db = pool) {
  const { rows } = await db.query(
    `INSERT INTO assinaturas_plataforma
       (condominio_id, plano_nome, plano_assinatura_id, asaas_assinatura_id, asaas_cliente_id,
        forma, valor_centavos, ativada_por_id)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
     RETURNING ${CAMPOS_ASSINATURA}`,
    [d.condominioId, d.planoNome, d.planoAssinaturaId ?? null, d.asaasAssinaturaId, d.asaasClienteId,
      d.forma, d.valorCentavos, d.ativadaPorId],
  );
  return rows[0];
}

export async function assinaturaPorId(id, db = pool) {
  const { rows } = await db.query(`SELECT ${CAMPOS_ASSINATURA} FROM assinaturas_plataforma WHERE id = $1`, [id]);
  return rows[0] ?? null;
}

export async function assinaturaPorAsaasId(asaasAssinaturaId, db = pool) {
  const { rows } = await db.query(
    `SELECT ${CAMPOS_ASSINATURA} FROM assinaturas_plataforma WHERE asaas_assinatura_id = $1`,
    [asaasAssinaturaId],
  );
  return rows[0] ?? null;
}

export async function ativaDoCondominio(condominioId, db = pool) {
  const { rows } = await db.query(
    `SELECT ${CAMPOS_ASSINATURA} FROM assinaturas_plataforma WHERE condominio_id = $1 AND status = 'ATIVA'`,
    [condominioId],
  );
  return rows[0] ?? null;
}

/** A ativa do condomínio; sem ela, a última cancelada, para o histórico. */
export async function maisRelevanteDoCondominio(condominioId, db = pool) {
  const { rows } = await db.query(
    `SELECT ${CAMPOS_ASSINATURA} FROM assinaturas_plataforma
      WHERE condominio_id = $1
      ORDER BY (status = 'ATIVA') DESC, criado_em DESC
      LIMIT 1`,
    [condominioId],
  );
  return rows[0] ?? null;
}

export async function listarAssinaturas(db = pool) {
  const { rows } = await db.query(
    `SELECT ${CAMPOS_ASSINATURA} FROM assinaturas_plataforma ORDER BY (status = 'ATIVA') DESC, criado_em DESC`,
  );
  return rows;
}

/**
 * Encerra a assinatura e cancela as cobranças que ainda não foram pagas — o
 * Asaas apaga as pendentes quando a assinatura é removida.
 */
export async function cancelarAssinatura(id, db = pool) {
  const { rows } = await db.query(
    `UPDATE assinaturas_plataforma SET status = 'CANCELADA', cancelada_em = now()
      WHERE id = $1 AND status = 'ATIVA'
      RETURNING ${CAMPOS_ASSINATURA}`,
    [id],
  );
  if (rows[0]) {
    await db.query(
      `UPDATE pagamentos_plataforma SET status = 'CANCELADA', atualizado_em = now()
        WHERE assinatura_id = $1 AND status IN ('PENDENTE', 'VENCIDA')`,
      [id],
    );
  }
  return rows[0] ?? null;
}

/**
 * Grava ou atualiza uma cobrança da assinatura, pelo id do Asaas.
 *
 * Webhook e conciliação chegam pela mesma porta, em qualquer ordem, quantas
 * vezes vierem: `ON CONFLICT` torna a escrita repetível. Estornada não volta
 * atrás — uma conciliação atrasada não desfaz um estorno já concluído.
 */
export async function registrarPagamento(assinaturaId, p, db = pool) {
  const { rows } = await db.query(
    `INSERT INTO pagamentos_plataforma
       (assinatura_id, asaas_pagamento_id, valor_centavos, vencimento, status, forma, url_pagamento, pago_em)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
     ON CONFLICT (asaas_pagamento_id) DO UPDATE
        SET valor_centavos = EXCLUDED.valor_centavos,
            vencimento     = EXCLUDED.vencimento,
            status         = CASE WHEN pagamentos_plataforma.status = 'ESTORNADA'
                                  THEN 'ESTORNADA' ELSE EXCLUDED.status END,
            forma          = COALESCE(EXCLUDED.forma, pagamentos_plataforma.forma),
            url_pagamento  = COALESCE(EXCLUDED.url_pagamento, pagamentos_plataforma.url_pagamento),
            pago_em        = COALESCE(EXCLUDED.pago_em, pagamentos_plataforma.pago_em),
            atualizado_em  = now()
     RETURNING ${CAMPOS_PAGAMENTO}`,
    [assinaturaId, p.asaasPagamentoId, p.valorCentavos, p.vencimento, p.status,
      p.forma, p.urlPagamento, p.pagoEm],
  );
  return rows[0];
}

export async function pagamentoPorId(id, db = pool) {
  const { rows } = await db.query(`SELECT ${CAMPOS_PAGAMENTO} FROM pagamentos_plataforma WHERE id = $1`, [id]);
  return rows[0] ?? null;
}

export async function pagamentoPorAsaasId(asaasPagamentoId, db = pool) {
  const { rows } = await db.query(
    `SELECT ${CAMPOS_PAGAMENTO} FROM pagamentos_plataforma WHERE asaas_pagamento_id = $1`,
    [asaasPagamentoId],
  );
  return rows[0] ?? null;
}

export async function marcarPagamento(id, status, db = pool) {
  await db.query(
    `UPDATE pagamentos_plataforma SET status = $2, atualizado_em = now() WHERE id = $1`,
    [id, status],
  );
}

export async function pagamentosDaAssinatura(assinaturaId, db = pool) {
  const { rows } = await db.query(
    `SELECT ${CAMPOS_PAGAMENTO} FROM pagamentos_plataforma
      WHERE assinatura_id = $1 ORDER BY vencimento DESC`,
    [assinaturaId],
  );
  return rows;
}
