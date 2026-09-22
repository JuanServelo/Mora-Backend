import { consultar } from '../config/database.js';

/**
 * Registra a confirmação de leitura.
 *
 * `ON CONFLICT DO NOTHING` e depois um SELECT: confirmar duas vezes é a mesma
 * confirmação, e a data que vale é a da primeira. Devolver a segunda como nova
 * faria o relatório do síndico mostrar o morador lendo o aviso toda vez que
 * abrisse a tela.
 */
export async function confirmar({ condominioId, avisoId, usuarioId }) {
  await consultar(
    `INSERT INTO aviso_leituras (condominio_id, aviso_id, usuario_id)
     VALUES ($1, $2, $3)
     ON CONFLICT (aviso_id, usuario_id) DO NOTHING`,
    [condominioId, avisoId, usuarioId],
  );

  const { rows } = await consultar(
    `SELECT id, condominio_id AS "condominioId", aviso_id AS "avisoId",
            usuario_id AS "usuarioId", confirmada_em AS "confirmadaEm"
       FROM aviso_leituras WHERE aviso_id = $1 AND usuario_id = $2`,
    [avisoId, usuarioId],
  );
  return rows[0];
}

/** Quem confirmou a leitura de um aviso. */
export async function porAviso(avisoId) {
  const { rows } = await consultar(
    `SELECT usuario_id AS "usuarioId", confirmada_em AS "confirmadaEm"
       FROM aviso_leituras WHERE aviso_id = $1
      ORDER BY confirmada_em ASC`,
    [avisoId],
  );
  return rows;
}

/** Avisos que este usuário já confirmou, para a tela marcar os pendentes. */
export async function avisosConfirmadosPor(usuarioId, condominioId) {
  const { rows } = await consultar(
    `SELECT aviso_id AS "avisoId", confirmada_em AS "confirmadaEm"
       FROM aviso_leituras WHERE usuario_id = $1 AND condominio_id = $2`,
    [usuarioId, condominioId],
  );
  return rows;
}

/** Contagem por aviso, para listar vários sem uma consulta por linha. */
export async function contarPorAvisos(avisoIds) {
  if (avisoIds.length === 0) return new Map();
  const { rows } = await consultar(
    `SELECT aviso_id AS "avisoId", COUNT(*)::int AS total
       FROM aviso_leituras WHERE aviso_id = ANY($1::uuid[])
      GROUP BY aviso_id`,
    [avisoIds],
  );
  return new Map(rows.map((r) => [r.avisoId, r.total]));
}
