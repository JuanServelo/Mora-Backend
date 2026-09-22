import { SERVICOS } from '../config/servicos.js';
import { buscar } from '../utils/httpClient.js';

/**
 * Quem são os usuários de um condomínio.
 *
 * Usado em dois lugares: para a gestão escolher o destinatário de uma conversa
 * direta, e para o relatório de leitura saber quantas pessoas deveriam ter
 * lido um aviso — sem o total, "8 confirmaram" não diz nada.
 *
 * Devolve o condomínio inteiro só para a gestão. O auth-api **aceita** a rota
 * para morador, mas recorta o resultado na unidade dele — Ana pede a lista e
 * recebe a si mesma e a convidada dela, duas linhas. Verificado com token real.
 *
 * É por isso que a conversa do morador é endereçada à *administração* e não a
 * uma pessoa: o síndico nunca aparece na lista que ele alcança.
 */
export async function listarUsuariosDoCondominio(condominioId, authorization) {
  const url = `${SERVICOS.auth}/api/user-management/users?condominioId=${encodeURIComponent(condominioId)}`;
  const r = await buscar(url, authorization);
  if (!r.ok) return null;
  return (r.dados?.usuarios ?? []).map((u) => ({
    id: u.id,
    nome: u.nome ?? u.email,
    email: u.email,
    perfil: u.perfil,
    // Caminho relativo servido pelo auth-api; quem monta a URL completa é a
    // tela, que conhece a origem do serviço.
    fotoUrl: u.fotoUrl ?? null,
    unidadeId: u.unidadeId ?? null,
    // `status` e `semAcessoSistema` sao o que o auth-api devolve; nao existe
    // campo `ativo`. Quem nao acessa o sistema — o morador cadastrado so para
    // liberar entrada — nao entra em lista de destinatario nem conta como
    // pessoa que deveria ler um aviso.
    alcancavel: u.status === 'active' && u.semAcessoSistema !== true,
  }));
}

/**
 * Confere que o usuário existe e pertence ao mesmo condomínio de quem chama.
 *
 * Vale a consulta antes de criar conversa direta: sem ela, a gestão poderia
 * abrir conversa com um id de outro condomínio só digitando o número.
 */
export async function usuarioDoCondominio(usuarioId, condominioId, authorization) {
  const lista = await listarUsuariosDoCondominio(condominioId, authorization);
  if (lista === null) return null;
  return lista.find((u) => String(u.id) === String(usuarioId)) ?? undefined;
}
