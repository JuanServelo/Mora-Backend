import { consultar } from '../config/database.js';

// Datas como texto pelo mesmo motivo de `multasModel`: DATE convertida em
// `Date` pode mudar de dia ao virar JSON.
const CAMPOS = `id, condominio_id AS "condominioId", unidade_id AS "unidadeId",
  proprietario_usuario_id AS "proprietarioUsuarioId",
  inquilino_usuario_id AS "inquilinoUsuarioId",
  inicio::text AS inicio, fim::text AS fim,
  valor_aluguel_centavos AS "valorAluguelCentavos",
  responsavel_taxas AS "responsavelTaxas", ativo,
  criado_em AS "criadoEm", atualizado_em AS "atualizadoEm"`;

/** Contratos do condomínio, ou de uma unidade. Vigentes primeiro. */
export async function listar(condominioId, { unidadeId } = {}) {
  const params = [condominioId];
  let filtro = '';
  if (unidadeId) {
    params.push(unidadeId);
    filtro = 'AND unidade_id = $2';
  }
  const { rows } = await consultar(
    `SELECT ${CAMPOS} FROM contratos_locacao
      WHERE condominio_id = $1 ${filtro}
      ORDER BY ativo DESC, inicio DESC`,
    params,
  );
  return rows;
}

export async function porId(condominioId, id) {
  const { rows } = await consultar(
    `SELECT ${CAMPOS} FROM contratos_locacao WHERE condominio_id = $1 AND id = $2`,
    [condominioId, id],
  );
  return rows[0] ?? null;
}

/**
 * Cria o contrato. O banco recusa em dois casos, e a função devolve o motivo
 * em vez de deixar o erro subir:
 * - `{ conflito: 'vigente' }`: a unidade já tem um contrato ativo
 *   (`uq_contrato_ativo`, 23505);
 * - `{ conflito: 'periodo' }`: o período cruza o de outro contrato da unidade,
 *   mesmo encerrado (`ex_contrato_periodo_unico`, 23P01).
 */
export async function criar(condominioId, d) {
  try {
    const { rows } = await consultar(
      `INSERT INTO contratos_locacao
         (condominio_id, unidade_id, proprietario_usuario_id, inquilino_usuario_id,
          inicio, fim, valor_aluguel_centavos, responsavel_taxas)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
       RETURNING ${CAMPOS}`,
      [condominioId, d.unidadeId, d.proprietarioUsuarioId, d.inquilinoUsuarioId ?? null,
        d.inicio, d.fim ?? null, d.valorAluguelCentavos ?? null, d.responsavelTaxas],
    );
    return rows[0];
  } catch (err) {
    if (err.code === '23505') return { conflito: 'vigente' };
    if (err.code === '23P01') return { conflito: 'periodo' };
    throw err;
  }
}

/**
 * Encerra: deixa de ser vigente, e o fim passa a ser hoje — ou fica o que já
 * estava, se for anterior.
 *
 * `LEAST` porque um contrato encerrado antes do prazo acabou hoje, não na data
 * prevista. Mantido o fim futuro, o período dele cruzaria o do contrato
 * seguinte da unidade, e a restrição `ex_contrato_periodo_unico` recusaria o
 * novo inquilino.
 *
 * `GREATEST` porque um contrato que ainda não começou, encerrado hoje, teria
 * fim antes do início — e a constraint `ck_contrato_vigencia` recusaria.
 */
export async function encerrar(condominioId, id, fim) {
  const { rows } = await consultar(
    `UPDATE contratos_locacao
        SET ativo = false, fim = GREATEST(inicio, LEAST(COALESCE(fim, $3::date), $3::date)),
            atualizado_em = now()
      WHERE condominio_id = $1 AND id = $2 AND ativo
      RETURNING ${CAMPOS}`,
    [condominioId, id, fim],
  );
  return rows[0] ?? null;
}
