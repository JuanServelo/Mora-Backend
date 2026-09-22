import { consultar } from '../config/database.js';

const CAMPOS = `
  id, condominio_id AS "condominioId", usuario_id AS "usuarioId",
  origem, tipo, titulo, mensagem, dados,
  lida_em AS "lidaEm", criado_em AS "criadoEm"
`;

/**
 * Grava uma notificação.
 *
 * `chaveUnica` é a defesa contra repetição: o job de faturamento pode rodar
 * duas vezes na mesma competência, e sem ela o morador receberia o mesmo aviso
 * de fatura de novo. `ON CONFLICT DO NOTHING` devolve zero linhas nesse caso —
 * quem chama trata como "já existia", não como erro.
 */
export async function inserir(n) {
  // `criadoEm` e `lidaEm` normalmente ficam a cargo do banco. Vêm preenchidos
  // só quando outro serviço publica um evento que já aconteceu — migração de
  // histórico, por exemplo. `COALESCE` mantém o padrão quando não vêm.
  const { rows } = await consultar(
    `INSERT INTO notificacoes
       (condominio_id, usuario_id, origem, tipo, titulo, mensagem, dados, chave_unica,
        criado_em, lida_em)
     VALUES ($1, $2, $3, $4, $5, $6, $7::jsonb, $8, COALESCE($9::timestamptz, now()), $10)
     ON CONFLICT (chave_unica) WHERE chave_unica IS NOT NULL DO NOTHING
     RETURNING ${CAMPOS}`,
    [
      n.condominioId,
      n.usuarioId,
      n.origem,
      n.tipo,
      n.titulo,
      n.mensagem,
      JSON.stringify(n.dados ?? {}),
      n.chaveUnica ?? null,
      n.criadoEm ?? null,
      n.lidaEm ?? null,
    ],
  );
  return rows[0] ?? null;
}

/** Caixa de entrada do próprio usuário, mais nova primeiro. */
export async function listarDoUsuario(usuarioId, { apenasNaoLidas = false, limite = 50, antesDe = null } = {}) {
  const condicoes = ['usuario_id = $1'];
  const valores = [usuarioId];

  if (apenasNaoLidas) condicoes.push('lida_em IS NULL');
  if (antesDe) {
    valores.push(antesDe);
    condicoes.push(`criado_em < $${valores.length}`);
  }

  valores.push(limite);

  const { rows } = await consultar(
    `SELECT ${CAMPOS} FROM notificacoes
      WHERE ${condicoes.join(' AND ')}
      ORDER BY criado_em DESC
      LIMIT $${valores.length}`,
    valores,
  );
  return rows;
}

/**
 * Quantas não lidas o usuário tem.
 *
 * `::int` de propósito: `COUNT()` é BIGINT, e o driver entrega BIGINT como
 * string. Sem o cast, `naoLidas + 1` viraria concatenação — o mesmo defeito
 * que inflou os KPIs do financeiro em dez vezes.
 */
export async function contarNaoLidas(usuarioId) {
  const { rows } = await consultar(
    `SELECT COUNT(*)::int AS total FROM notificacoes
      WHERE usuario_id = $1 AND lida_em IS NULL`,
    [usuarioId],
  );
  return rows[0].total;
}

/** Marca uma notificação como lida. O filtro por usuário é a autorização. */
export async function marcarLida(id, usuarioId) {
  const { rows } = await consultar(
    `UPDATE notificacoes
        SET lida_em = COALESCE(lida_em, now())
      WHERE id = $1 AND usuario_id = $2
      RETURNING ${CAMPOS}`,
    [id, usuarioId],
  );
  return rows[0] ?? null;
}

export async function marcarTodasLidas(usuarioId) {
  const { rowCount } = await consultar(
    `UPDATE notificacoes SET lida_em = now()
      WHERE usuario_id = $1 AND lida_em IS NULL`,
    [usuarioId],
  );
  return rowCount;
}
