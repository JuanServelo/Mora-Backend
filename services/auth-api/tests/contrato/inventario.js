import { PERFIS as P } from '../../constants/perfis.js';

/**
 * O contrato de acesso de cada rota do auth-api.
 *
 * Existe por dois motivos, e o segundo é o que importa:
 *
 * 1. É a lista sobre a qual os testes de contrato iteram — cada rota é
 *    exercitada, não uma amostra delas.
 * 2. **Rota nova obriga uma decisão explícita.** `inventario.test.js` compara
 *    esta tabela com o que o Express registrou de fato: um endpoint que nasce
 *    sem entrada aqui quebra a suíte, e quem o escreveu precisa dizer, por
 *    escrito, se é público e quem ele recusa. Sem isso, uma rota administrativa
 *    exposta por engano passa despercebida — ninguém revisa o que não aparece.
 *
 * Nada aqui foi escrito de memória. Cada rota foi chamada sem token e com um
 * token de cada perfil, e o resultado observado virou a entrada; depois cada
 * linha foi revisada. O que se congela é o comportamento real, e é essa
 * diferença que faz a tabela detectar mudança em vez de repeti-la.
 *
 * `nega` lista os perfis que recebem **403 do middleware**, antes de qualquer
 * consulta. Uma lista vazia não quer dizer "liberado para todos": quer dizer
 * que a decisão não é de perfil. Várias dessas rotas recortam o resultado pelo
 * condomínio ou pela unidade de quem pediu, dentro do handler — e isso só é
 * verificável com banco, nos testes de integração.
 */

/** Responde sem token. Login, ativação por convite, OAuth e health. */
export const PUBLICA = 'publica';

/** Exige `Authorization: Bearer`. Sem ele, 401 antes de qualquer consulta. */
export const TOKEN = 'token';

export const INVENTARIO = {
  'POST /api/auth/forgot-password': { acesso: PUBLICA },
  'GET /api/auth/google': { acesso: PUBLICA },
  'GET /api/auth/google/callback': { acesso: PUBLICA },
  'GET /api/auth/google/disponivel': { acesso: PUBLICA },
  'POST /api/auth/login': { acesso: PUBLICA },
  'POST /api/auth/logout': { acesso: TOKEN, nega: [] },
  'GET /api/auth/me': { acesso: TOKEN, nega: [] },
  'PUT /api/auth/me': { acesso: TOKEN, nega: [] },
  'POST /api/auth/me/foto': { acesso: TOKEN, nega: [] },
  'POST /api/auth/oauth/exchange': { acesso: PUBLICA },
  'POST /api/auth/register': { acesso: PUBLICA },
  'POST /api/auth/reset-password/:token': { acesso: PUBLICA },
  'GET /api/condominios': { acesso: TOKEN, nega: [] },
  'POST /api/condominios': { acesso: TOKEN, nega: [P.ADMIN_SINDICO, P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
  'GET /api/condominios/:id': { acesso: TOKEN, nega: [] },
  'PUT /api/condominios/:id': { acesso: TOKEN, nega: [P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
  'PATCH /api/condominios/:id/activate': { acesso: TOKEN, nega: [P.ADMIN_SINDICO, P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
  'PATCH /api/condominios/:id/assign-user': { acesso: TOKEN, nega: [P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
  'PATCH /api/condominios/:id/deactivate': { acesso: TOKEN, nega: [P.ADMIN_SINDICO, P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
  'GET /api/condominios/:id/users': { acesso: TOKEN, nega: [P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
  'GET /api/estatisticas/condominios/:id': { acesso: TOKEN, nega: [P.ADMIN_SINDICO, P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
  'GET /api/estatisticas/plataforma': { acesso: TOKEN, nega: [P.ADMIN_SINDICO, P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
  'GET /api/health': { acesso: PUBLICA },
  'POST /api/invites/activate': { acesso: PUBLICA },
  'POST /api/invites/validate': { acesso: PUBLICA },
  'GET /api/perfis/info': { acesso: TOKEN, nega: [P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
  'GET /api/perfis/info/:perfil': { acesso: TOKEN, nega: [P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
  'GET /api/portaria/dentro': { acesso: TOKEN, nega: [P.MORADOR, P.CONVIDADO] },
  'POST /api/portaria/entrada/:userId': { acesso: TOKEN, nega: [P.MORADOR, P.CONVIDADO] },
  'GET /api/portaria/funcionarios': { acesso: TOKEN, nega: [P.MORADOR, P.CONVIDADO] },
  'GET /api/portaria/guests': { acesso: TOKEN, nega: [P.MORADOR, P.CONVIDADO] },
  'PATCH /api/portaria/guests/:guestId/permissao': { acesso: TOKEN, nega: [P.PORTEIRO, P.CONVIDADO] },
  'GET /api/portaria/historico-acesso': { acesso: TOKEN, nega: [P.MORADOR, P.CONVIDADO] },
  'GET /api/portaria/historico/:userId': { acesso: TOKEN, nega: [P.MORADOR, P.CONVIDADO] },
  'GET /api/portaria/meus-guests': { acesso: TOKEN, nega: [P.PORTEIRO, P.CONVIDADO] },
  'GET /api/portaria/pessoas-unidade': { acesso: TOKEN, nega: [P.CONVIDADO] },
  'GET /api/portaria/residentes': { acesso: TOKEN, nega: [P.MORADOR, P.CONVIDADO] },
  'POST /api/portaria/saida/:userId': { acesso: TOKEN, nega: [P.MORADOR, P.CONVIDADO] },
  'GET /api/portaria/usuarios-condominio': { acesso: TOKEN, nega: [P.MORADOR, P.CONVIDADO] },
  'POST /api/reclamacoes': { acesso: TOKEN, nega: [] },
  'PATCH /api/reclamacoes/:id': { acesso: TOKEN, nega: [P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
  'GET /api/reclamacoes/minhas': { acesso: TOKEN, nega: [] },
  'GET /api/reclamacoes/todas': { acesso: TOKEN, nega: [P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
  'POST /api/user-management/invites': { acesso: TOKEN, nega: [P.PORTEIRO, P.CONVIDADO] },
  'POST /api/user-management/invites/:id/resend': { acesso: TOKEN, nega: [P.PORTEIRO, P.CONVIDADO] },
  'GET /api/user-management/units/:unidadeId/occupants': { acesso: TOKEN, nega: [P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
  'DELETE /api/user-management/units/:unidadeId/occupants/:userId': { acesso: TOKEN, nega: [P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
  'POST /api/user-management/units/:unidadeId/occupants/guest': { acesso: TOKEN, nega: [P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
  'POST /api/user-management/units/:unidadeId/occupants/lessee': { acesso: TOKEN, nega: [P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
  'POST /api/user-management/units/:unidadeId/occupants/occupant': { acesso: TOKEN, nega: [P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
  'GET /api/user-management/units/:unidadeId/residents': { acesso: TOKEN, nega: [P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
  'GET /api/user-management/units/:unidadeId/transfer-eligibility': { acesso: TOKEN, nega: [P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
  'POST /api/user-management/units/:unidadeId/transfer-financial-responsibility': { acesso: TOKEN, nega: [P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
  'GET /api/user-management/users': { acesso: TOKEN, nega: [P.PORTEIRO, P.CONVIDADO] },
  'PATCH /api/user-management/users/:id/deactivate': { acesso: TOKEN, nega: [P.PORTEIRO, P.CONVIDADO] },
  'DELETE /api/user-management/users/:id/unit': { acesso: TOKEN, nega: [P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
  'PATCH /api/user-management/users/:id/unit': { acesso: TOKEN, nega: [P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
  'GET /api/users': { acesso: TOKEN, nega: [P.PORTEIRO, P.CONVIDADO] },
  'POST /api/users': { acesso: TOKEN, nega: [P.PORTEIRO, P.CONVIDADO] },
  'DELETE /api/users/:id': { acesso: TOKEN, nega: [P.PORTEIRO, P.CONVIDADO] },
  'PUT /api/users/:id': { acesso: TOKEN, nega: [P.PORTEIRO, P.MORADOR, P.CONVIDADO] },
};

/** As rotas de um tipo de acesso. */
export function rotasCom(acesso) {
  return Object.entries(INVENTARIO)
    .filter(([, v]) => v.acesso === acesso)
    .map(([k]) => k);
}

/** As rotas que recusam pelo menos um perfil, como `[chave, perfis]`. */
export function rotasComBarreiraDePerfil() {
  return Object.entries(INVENTARIO)
    .filter(([, v]) => v.nega?.length)
    .map(([k, v]) => [k, v.nega]);
}

/** `GET /api/auth/me` -> `{ metodo, caminho }`. */
export function separar(chave) {
  const [metodo, ...resto] = chave.split(' ');
  return { metodo, caminho: resto.join(' ') };
}

/**
 * Caminho com os parâmetros preenchidos, pronto para chamar.
 *
 * O valor é um UUID porque várias rotas validam o formato antes de consultar o
 * banco: um `:id` com "1" dentro daria 400 de validação, e o teste leria isso
 * como se a autorização tivesse passado.
 */
export function comParametros(caminho) {
  return caminho.replace(/:[a-zA-Z]+/g, '11111111-1111-1111-1111-111111111111');
}
