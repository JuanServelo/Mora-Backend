import { consultar } from '../config/database.js';

const CAMPOS = `id, condominio_id AS "condominioId", nome, descricao,
                valor_centavos AS "valorCentavos", base_calculo AS "baseCalculo",
                periodicidade, extraordinaria, parcelas, ativo,
                criado_em AS "criadoEm"`;

export async function listar(condominioId, { incluirInativos = false } = {}) {
  const { rows } = await consultar(
    `SELECT ${CAMPOS} FROM tipos_taxa
      WHERE condominio_id = $1 ${incluirInativos ? '' : 'AND ativo'}
      ORDER BY ativo DESC, nome`,
    [condominioId],
  );
  return rows;
}

export async function porId(condominioId, id) {
  const { rows } = await consultar(
    `SELECT ${CAMPOS} FROM tipos_taxa WHERE condominio_id = $1 AND id = $2`,
    [condominioId, id],
  );
  return rows[0] ?? null;
}

export async function criar(condominioId, t) {
  const { rows } = await consultar(
    `INSERT INTO tipos_taxa
       (condominio_id, nome, descricao, valor_centavos, base_calculo,
        periodicidade, extraordinaria, parcelas)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
     RETURNING ${CAMPOS}`,
    [condominioId, t.nome, t.descricao ?? null, t.valorCentavos, t.baseCalculo,
      t.periodicidade, t.extraordinaria, t.parcelas],
  );
  return rows[0];
}

export async function atualizar(condominioId, id, t) {
  const { rows } = await consultar(
    `UPDATE tipos_taxa
        SET nome = $3, descricao = $4, valor_centavos = $5, base_calculo = $6,
            periodicidade = $7, extraordinaria = $8, parcelas = $9,
            ativo = $10, atualizado_em = now()
      WHERE condominio_id = $1 AND id = $2
      RETURNING ${CAMPOS}`,
    [condominioId, id, t.nome, t.descricao ?? null, t.valorCentavos, t.baseCalculo,
      t.periodicidade, t.extraordinaria, t.parcelas, t.ativo],
  );
  return rows[0] ?? null;
}

/**
 * Desativa em vez de apagar.
 *
 * Faturas emitidas apontam para o tipo de taxa que as originou; apagar a linha
 * deixaria a composição de uma fatura antiga sem explicação.
 */
export async function desativar(condominioId, id) {
  const { rows } = await consultar(
    `UPDATE tipos_taxa SET ativo = false, atualizado_em = now()
      WHERE condominio_id = $1 AND id = $2 RETURNING ${CAMPOS}`,
    [condominioId, id],
  );
  return rows[0] ?? null;
}
