import { consultar } from '../config/database.js';

// `data_infracao` sai como texto: o driver transformaria a DATE num `Date` à
// meia-noite local, e a serialização em UTC poderia trocar o dia.
const CAMPOS = `id, condominio_id AS "condominioId", unidade_id AS "unidadeId",
  usuario_id AS "usuarioId", valor_centavos AS "valorCentavos", motivo,
  data_infracao::text AS "dataInfracao", status, ocorrencia_id AS "ocorrenciaId",
  aplicada_por_id AS "aplicadaPorId",
  recurso_texto AS "recursoTexto", recurso_em AS "recursoEm",
  julgado_por_id AS "julgadoPorId", julgado_em AS "julgadoEm",
  julgamento_justificativa AS "julgamentoJustificativa",
  fatura_item_id AS "faturaItemId",
  estorno_fatura_item_id AS "estornoFaturaItemId",
  criado_em AS "criadoEm", atualizado_em AS "atualizadoEm"`;

/** Multas do condomínio, mais recentes primeiro. Filtros opcionais. */
export async function listar(condominioId, { status, unidadeId } = {}) {
  const params = [condominioId];
  const filtros = [];
  if (status) {
    params.push(status);
    filtros.push(`status = $${params.length}`);
  }
  if (unidadeId) {
    params.push(unidadeId);
    filtros.push(`unidade_id = $${params.length}`);
  }
  const where = filtros.length ? `AND ${filtros.join(' AND ')}` : '';

  const { rows } = await consultar(
    `SELECT ${CAMPOS} FROM multas
      WHERE condominio_id = $1 ${where}
      ORDER BY criado_em DESC`,
    params,
  );
  return rows;
}

export async function porId(condominioId, id) {
  const { rows } = await consultar(
    `SELECT ${CAMPOS} FROM multas WHERE condominio_id = $1 AND id = $2`,
    [condominioId, id],
  );
  return rows[0] ?? null;
}

export async function criar(condominioId, d) {
  const { rows } = await consultar(
    `INSERT INTO multas
       (condominio_id, unidade_id, usuario_id, valor_centavos, motivo,
        data_infracao, aplicada_por_id)
     VALUES ($1, $2, $3, $4, $5, $6, $7)
     RETURNING ${CAMPOS}`,
    [condominioId, d.unidadeId, d.usuarioId ?? null, d.valorCentavos, d.motivo,
      d.dataInfracao, d.aplicadaPorId],
  );
  return rows[0];
}

/*
 * As mudanças de status abaixo levam o status esperado no WHERE.
 *
 * O service já confere a transição antes, mas entre a leitura e a escrita outra
 * requisição pode ter mudado a multa — dois cliques em "recusar", ou o síndico
 * cancelando enquanto o morador recorre. Com a condição no UPDATE, a segunda
 * escrita não acha linha e volta nula, em vez de sobrescrever a primeira.
 */

export async function registrarRecurso(condominioId, id, texto) {
  const { rows } = await consultar(
    `UPDATE multas
        SET status = 'EM_RECURSO', recurso_texto = $3, recurso_em = now(),
            atualizado_em = now()
      WHERE condominio_id = $1 AND id = $2
        AND status = 'APLICADA' AND recurso_texto IS NULL AND fatura_item_id IS NULL
      RETURNING ${CAMPOS}`,
    [condominioId, id, texto],
  );
  return rows[0] ?? null;
}

export async function julgar(condominioId, id, novoStatus, julgadoPorId, justificativa) {
  const { rows } = await consultar(
    `UPDATE multas
        SET status = $3, julgado_por_id = $4, julgado_em = now(),
            julgamento_justificativa = $5, atualizado_em = now()
      WHERE condominio_id = $1 AND id = $2 AND status = 'EM_RECURSO'
      RETURNING ${CAMPOS}`,
    [condominioId, id, novoStatus, julgadoPorId, justificativa],
  );
  return rows[0] ?? null;
}

export async function cancelar(condominioId, id, julgadoPorId, justificativa) {
  const { rows } = await consultar(
    `UPDATE multas
        SET status = 'CANCELADA', julgado_por_id = $3, julgado_em = now(),
            julgamento_justificativa = $4, atualizado_em = now()
      WHERE condominio_id = $1 AND id = $2 AND status IN ('APLICADA', 'EM_RECURSO')
      RETURNING ${CAMPOS}`,
    [condominioId, id, julgadoPorId, justificativa],
  );
  return rows[0] ?? null;
}

/**
 * Candidatas do fechamento: aplicadas ainda não cobradas, e canceladas que
 * foram cobradas e ainda não estornadas.
 *
 * O prazo de recurso é conferido depois, em `utils/multas.js`, porque depende
 * das regras do condomínio e da data do fechamento.
 */
export async function listarParaFaturamento(condominioId) {
  const { rows } = await consultar(
    `SELECT ${CAMPOS} FROM multas
      WHERE condominio_id = $1
        AND ((status = 'APLICADA' AND fatura_item_id IS NULL)
          OR (status = 'CANCELADA' AND fatura_item_id IS NOT NULL
              AND estorno_fatura_item_id IS NULL))`,
    [condominioId],
  );
  return rows;
}

/** Marca a multa como cobrada. Roda na mesma transação que cria o item. */
export async function vincularItem(cliente, multaId, faturaItemId) {
  await cliente.query(
    `UPDATE multas SET fatura_item_id = $2, atualizado_em = now()
      WHERE id = $1 AND fatura_item_id IS NULL`,
    [multaId, faturaItemId],
  );
}

/** Marca o estorno como lançado. Roda na mesma transação que cria o item. */
export async function vincularEstorno(cliente, multaId, faturaItemId) {
  await cliente.query(
    `UPDATE multas SET estorno_fatura_item_id = $2, atualizado_em = now()
      WHERE id = $1 AND estorno_fatura_item_id IS NULL`,
    [multaId, faturaItemId],
  );
}
