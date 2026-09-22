import { consultar } from '../config/database.js';

const CAMPOS = `
  id, conversa_id AS "conversaId", autor_id AS "autorId",
  autor_perfil AS "autorPerfil", corpo,
  removida_em AS "removidaEm", criada_em AS "criadaEm"
`;

export async function inserir(cliente, { conversaId, autorId, autorPerfil, corpo }) {
  const { rows } = await cliente.query(
    `INSERT INTO mensagens (conversa_id, autor_id, autor_perfil, corpo)
     VALUES ($1, $2, $3, $4)
     RETURNING ${CAMPOS}`,
    [conversaId, autorId, autorPerfil, corpo],
  );
  return rows[0];
}

/** Carimba a conversa, para a listagem ordenar sem varrer as mensagens. */
export async function tocarConversa(cliente, conversaId, quando) {
  await cliente.query('UPDATE conversas SET ultima_mensagem_em = $2 WHERE id = $1', [
    conversaId,
    quando,
  ]);
}

export async function listarDaConversa(conversaId, { limite = 200 } = {}) {
  const { rows } = await consultar(
    `SELECT ${CAMPOS} FROM mensagens
      WHERE conversa_id = $1
      ORDER BY criada_em ASC
      LIMIT $2`,
    [conversaId, limite],
  );

  // A mensagem removida continua na conversa como marca, sem o texto. Sumir
  // com a linha deixaria a conversa incoerente para quem já tinha lido.
  return rows.map((m) => (m.removidaEm ? { ...m, corpo: null, removida: true } : { ...m, removida: false }));
}

/** Só o autor apaga a própria mensagem, e o filtro no WHERE é a autorização. */
export async function remover(id, autorId) {
  const { rows } = await consultar(
    `UPDATE mensagens SET removida_em = COALESCE(removida_em, now())
      WHERE id = $1 AND autor_id = $2
      RETURNING ${CAMPOS}`,
    [id, autorId],
  );
  return rows[0] ?? null;
}
