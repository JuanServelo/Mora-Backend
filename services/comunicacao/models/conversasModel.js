import { consultar } from '../config/database.js';

const CAMPOS = `
  c.id, c.condominio_id AS "condominioId", c.tipo, c.assunto,
  c.criada_por AS "criadaPor", c.ultima_mensagem_em AS "ultimaMensagemEm",
  c.encerrada_em AS "encerradaEm", c.criada_em AS "criadaEm"
`;

export async function criar(cliente, { condominioId, tipo, assunto, criadaPor }) {
  const { rows } = await cliente.query(
    `INSERT INTO conversas (condominio_id, tipo, assunto, criada_por)
     VALUES ($1, $2, $3, $4)
     RETURNING id, condominio_id AS "condominioId", tipo, assunto,
               criada_por AS "criadaPor", criada_em AS "criadaEm"`,
    [condominioId, tipo, assunto, criadaPor],
  );
  return rows[0];
}

export async function adicionarParticipante(cliente, conversaId, usuarioId) {
  // Idempotente: a gestão entra na conversa da administração ao responder, e
  // responder de novo não pode estourar por chave duplicada.
  await cliente.query(
    `INSERT INTO conversa_participantes (conversa_id, usuario_id)
     VALUES ($1, $2)
     ON CONFLICT (conversa_id, usuario_id) DO NOTHING`,
    [conversaId, usuarioId],
  );
}

export async function buscarPorId(id) {
  const { rows } = await consultar(`SELECT ${CAMPOS} FROM conversas c WHERE c.id = $1`, [id]);
  return rows[0] ?? null;
}

export async function participa(conversaId, usuarioId) {
  const { rows } = await consultar(
    'SELECT 1 FROM conversa_participantes WHERE conversa_id = $1 AND usuario_id = $2',
    [conversaId, usuarioId],
  );
  return rows.length > 0;
}

/**
 * Conversas visíveis para um usuário.
 *
 * São duas origens, e a segunda é a razão de a consulta ter UNION:
 *
 *   1. aquelas em que ele é participante nomeado;
 *   2. se for gestão, todas as `ADMINISTRACAO` do condomínio dele — mesmo as
 *      que ninguém da gestão abriu ainda. Sem isso, a primeira mensagem de um
 *      morador não apareceria para ninguém e ficaria esperando resposta para
 *      sempre.
 *
 * `naoLidas` conta pela marca d'água do participante. Para a gestão que ainda
 * não entrou na conversa não há marca, e `COALESCE` faz valer a data de
 * criação da conversa: tudo conta como não lido, que é o correto.
 */
export async function listarVisiveis(usuarioId, condominioId, ehGestao, { incluirEncerradas = false } = {}) {
  const filtroEncerrada = incluirEncerradas ? '' : 'AND c.encerrada_em IS NULL';

  const { rows } = await consultar(
    `WITH visiveis AS (
       SELECT c.id
         FROM conversas c
         JOIN conversa_participantes p ON p.conversa_id = c.id AND p.usuario_id = $1
        WHERE c.condominio_id = $2
       UNION
       SELECT c.id
         FROM conversas c
        WHERE $3::boolean
          AND c.tipo = 'ADMINISTRACAO'
          AND c.condominio_id = $2
     )
     SELECT ${CAMPOS},
            p.ultima_leitura_em AS "ultimaLeituraEm",
            (SELECT COUNT(*)::int
               FROM mensagens m
              WHERE m.conversa_id = c.id
                AND m.removida_em IS NULL
                AND m.autor_id <> $1
                AND m.criada_em > COALESCE(p.ultima_leitura_em, c.criada_em - interval '100 years')
            ) AS "naoLidas",
            (SELECT m.corpo FROM mensagens m
              WHERE m.conversa_id = c.id AND m.removida_em IS NULL
              ORDER BY m.criada_em DESC LIMIT 1) AS "ultimoCorpo",
            -- Quem participa, para a tela saber com quem é a conversa. Sem
            -- isso só dava para mostrar quem a criou — e numa conversa que a
            -- própria gestão abriu, quem criou é ela mesma.
            (SELECT array_agg(p2.usuario_id)
               FROM conversa_participantes p2
              WHERE p2.conversa_id = c.id) AS "participantes"
       FROM conversas c
       JOIN visiveis v ON v.id = c.id
       LEFT JOIN conversa_participantes p
              ON p.conversa_id = c.id AND p.usuario_id = $1
      WHERE TRUE ${filtroEncerrada}
      ORDER BY c.ultima_mensagem_em DESC NULLS LAST, c.criada_em DESC`,
    [usuarioId, condominioId, ehGestao],
  );
  return rows;
}

export async function participantesDe(conversaId) {
  const { rows } = await consultar(
    `SELECT usuario_id AS "usuarioId", ultima_leitura_em AS "ultimaLeituraEm",
            entrou_em AS "entrouEm"
       FROM conversa_participantes WHERE conversa_id = $1`,
    [conversaId],
  );
  return rows;
}

export async function marcarLeitura(conversaId, usuarioId) {
  await consultar(
    `UPDATE conversa_participantes SET ultima_leitura_em = now()
      WHERE conversa_id = $1 AND usuario_id = $2`,
    [conversaId, usuarioId],
  );
}

export async function encerrar(id, condominioId) {
  const { rows } = await consultar(
    `UPDATE conversas SET encerrada_em = COALESCE(encerrada_em, now())
      WHERE id = $1 AND condominio_id = $2
      RETURNING id, condominio_id AS "condominioId", tipo, assunto,
                criada_por AS "criadaPor", ultima_mensagem_em AS "ultimaMensagemEm",
                encerrada_em AS "encerradaEm", criada_em AS "criadaEm"`,
    [id, condominioId],
  );
  return rows[0] ?? null;
}
