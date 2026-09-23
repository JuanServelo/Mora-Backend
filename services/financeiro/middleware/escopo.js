import { identidadeDoAtor } from '../clients/authClient.js';

const PERFIS_UNIDADE = ['MORADOR', 'DONO_ALUGUEL'];

/**
 * Resolve o condomínio da requisição a partir das claims.
 *
 * A regra é a mesma já aplicada em `gestao-geral/routes/dashboard.js`: o
 * `condominioId` vem do token, nunca do corpo ou da query. O Admin Geral é o
 * único que escolhe qual condomínio olhar; para os demais o parâmetro é
 * ignorado, e não recusado — recusar diria a quem tentou que o outro
 * condomínio existe.
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
    // Resolvido sob demanda por `resolverUnidade`: o token não carrega unidade.
    unidadeId: null,
    responsavelFinanceiro: false,
    // O Admin Geral vê os números, mas não opera cobrança de ninguém.
    somenteLeitura: perfil === 'ADMIN_GERAL',
  };

  next();
}

/**
 * Completa o escopo com a unidade do ator, para as rotas que um morador acessa.
 *
 * O `condominioId` está na claim, mas o `unidadeId` não — quem emite o token é
 * o auth-api, e ele não o inclui. Perguntar é a única forma correta; deduzir
 * pela ausência seria o pior caminho, porque "sem unidade" viraria "sem
 * filtro", e um morador enxergaria as faturas do prédio inteiro.
 *
 * Por isso falha fechado: auth-api fora do ar é 503, não acesso liberado.
 * Use sempre depois de `resolverEscopo`.
 */
export async function resolverUnidade(req, res, next) {
  if (!PERFIS_UNIDADE.includes(req.perfil)) return next();

  const identidade = await identidadeDoAtor(req.claims.id, req.authorization);

  if (!identidade) {
    return res.status(503).json({
      sucesso: false,
      mensagem: 'Não foi possível confirmar seu vínculo com a unidade. Tente novamente.',
    });
  }

  if (!identidade.unidadeId) {
    return res.status(403).json({
      sucesso: false,
      mensagem: 'Seu usuário não está vinculado a uma unidade.',
    });
  }

  req.escopo.unidadeId = identidade.unidadeId;
  req.escopo.responsavelFinanceiro = identidade.responsavelFinanceiro;
  next();
}

/** Bloqueia escrita para quem só pode observar. */
export function exigirEscrita(req, res, next) {
  if (req.escopo?.somenteLeitura) {
    return res.status(403).json({
      sucesso: false,
      mensagem: 'O Admin Geral acompanha o financeiro dos condomínios, mas não opera cobranças.',
    });
  }
  next();
}
