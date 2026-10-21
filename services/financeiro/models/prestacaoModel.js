import { consultar } from '../config/database.js';

const CAMPOS_LANCAMENTO = `id, condominio_id AS "condominioId", competencia::text AS competencia,
  tipo, categoria, descricao, valor_centavos AS "valorCentavos",
  data_lancamento::text AS "dataLancamento", comprovante_url AS "comprovanteUrl",
  criado_por_id AS "criadoPorId", criado_em AS "criadoEm"`;

const CAMPOS_PRESTACAO = `competencia::text AS competencia, status,
  publicado_em AS "publicadoEm", publicado_por_id AS "publicadoPorId"`;

export async function listarLancamentos(condominioId, competencia) {
  const { rows } = await consultar(
    `SELECT ${CAMPOS_LANCAMENTO} FROM lancamentos
      WHERE condominio_id = $1 AND competencia = $2
      ORDER BY data_lancamento, criado_em`,
    [condominioId, competencia],
  );
  return rows;
}

/** Situação da competência. Sem linha, ainda é rascunho. */
export async function situacao(condominioId, competencia) {
  const { rows } = await consultar(
    `SELECT ${CAMPOS_PRESTACAO} FROM prestacao_contas
      WHERE condominio_id = $1 AND competencia = $2`,
    [condominioId, competencia],
  );
  return rows[0] ?? { competencia, status: 'RASCUNHO', publicadoEm: null, publicadoPorId: null };
}

/** Receita que as faturas pagas da competência trouxeram. */
export async function receitaDeTaxas(condominioId, competencia) {
  const { rows } = await consultar(
    `SELECT COALESCE(SUM(COALESCE(valor_pago_centavos, valor_centavos)), 0)::bigint AS total
       FROM faturas
      WHERE condominio_id = $1 AND competencia = $2 AND status = 'PAGA'`,
    [condominioId, competencia],
  );
  return rows[0].total;
}

/**
 * Lança uma receita ou despesa — só se a competência não estiver publicada.
 *
 * A condição está dentro do INSERT, e não numa leitura antes: entre "conferir"
 * e "gravar", o síndico poderia publicar em outra aba, e o lançamento entraria
 * numa prestação que os moradores já viram.
 */
//
// Os tipos vão explícitos porque `$1` e `$2` aparecem duas vezes — como valor
// do SELECT e na comparação do NOT EXISTS —, e sem o cast o Postgres deduz
// tipos diferentes para o mesmo parâmetro e recusa a consulta.
export async function lancar(condominioId, competencia, d) {
  const { rows } = await consultar(
    `INSERT INTO lancamentos
       (condominio_id, competencia, tipo, categoria, descricao, valor_centavos,
        data_lancamento, comprovante_url, criado_por_id)
     SELECT $1::varchar, $2::date, $3::varchar, $4::varchar, $5::varchar, $6::bigint,
            $7::date, $8::text, $9::integer
      WHERE NOT EXISTS (
        SELECT 1 FROM prestacao_contas
         WHERE condominio_id = $1::varchar AND competencia = $2::date AND status = 'PUBLICADA')
     RETURNING ${CAMPOS_LANCAMENTO}`,
    [condominioId, competencia, d.tipo, d.categoria, d.descricao, d.valorCentavos,
      d.dataLancamento, d.comprovanteUrl ?? null, d.criadoPorId],
  );
  return rows[0] ?? null;
}

/** Exclui um lançamento — mesma regra: só de competência não publicada. */
export async function excluirLancamento(condominioId, id) {
  const { rows } = await consultar(
    `DELETE FROM lancamentos l
      WHERE l.condominio_id = $1 AND l.id = $2
        AND NOT EXISTS (
          SELECT 1 FROM prestacao_contas p
           WHERE p.condominio_id = l.condominio_id AND p.competencia = l.competencia
             AND p.status = 'PUBLICADA')
      RETURNING l.id`,
    [condominioId, id],
  );
  return rows[0] ?? null;
}

export async function lancamentoPorId(condominioId, id) {
  const { rows } = await consultar(
    `SELECT ${CAMPOS_LANCAMENTO} FROM lancamentos WHERE condominio_id = $1 AND id = $2`,
    [condominioId, id],
  );
  return rows[0] ?? null;
}

/** Publica. Devolve nulo se já estava publicada. */
export async function publicar(condominioId, competencia, publicadoPorId) {
  const { rows } = await consultar(
    `INSERT INTO prestacao_contas (condominio_id, competencia, status, publicado_em, publicado_por_id)
     VALUES ($1, $2, 'PUBLICADA', now(), $3)
     ON CONFLICT (condominio_id, competencia) DO UPDATE
       SET status = 'PUBLICADA', publicado_em = now(), publicado_por_id = $3, atualizado_em = now()
       WHERE prestacao_contas.status <> 'PUBLICADA'
     RETURNING ${CAMPOS_PRESTACAO}`,
    [condominioId, competencia, publicadoPorId],
  );
  return rows[0] ?? null;
}

/** Competências publicadas, mais recentes primeiro — o que o morador enxerga. */
export async function listarPublicadas(condominioId) {
  const { rows } = await consultar(
    `SELECT ${CAMPOS_PRESTACAO} FROM prestacao_contas
      WHERE condominio_id = $1 AND status = 'PUBLICADA'
      ORDER BY competencia DESC`,
    [condominioId],
  );
  return rows;
}
