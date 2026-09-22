/**
 * Resolve o condomínio da requisição a partir das claims.
 *
 * Mesma regra do `financeiro` e do `gestao-geral`: o `condominioId` vem do
 * token, nunca do corpo ou da query. O Admin Geral é o único que escolhe qual
 * condomínio olhar; para os demais o parâmetro é ignorado, e não recusado —
 * recusar diria a quem tentou que o outro condomínio existe.
 */
export function resolverEscopo(req, res, next) {
  const { perfil, claims } = req;

  const condominioId = perfil === 'ADMIN_GERAL'
    ? (req.params.condominioId ?? req.query.condominioId ?? null)
    : (claims.condominioId ?? null);

  if (!condominioId) {
    const mensagem = perfil === 'ADMIN_GERAL'
      ? 'Informe o condomínio em condominioId.'
      : 'Seu usuário não está vinculado a um condomínio.';
    return res.status(400).json({ sucesso: false, mensagem });
  }

  req.escopo = {
    condominioId,
    usuarioId: claims.id,
    perfil,
    ehGestao: perfil === 'ADMIN_GERAL' || perfil === 'ADMIN_SINDICO',
    // O Admin Geral enxerga o condomínio para dar suporte; falar em nome da
    // administração é do síndico. Sem essa distinção, uma resposta da
    // plataforma apareceria para o morador como se fosse do condomínio dele.
    somenteLeitura: perfil === 'ADMIN_GERAL',
  };

  next();
}

/** Bloqueia escrita para quem só pode observar. */
export function exigirEscrita(req, res, next) {
  if (req.escopo?.somenteLeitura) {
    return res.status(403).json({
      sucesso: false,
      mensagem: 'O Admin Geral acompanha a comunicação dos condomínios, mas não responde por eles.',
    });
  }
  next();
}
