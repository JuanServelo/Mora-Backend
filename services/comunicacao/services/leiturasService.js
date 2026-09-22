import * as leituras from '../models/leiturasModel.js';
import { buscarAviso, listarAvisosAtivos } from '../clients/portariaClient.js';
import { listarUsuariosDoCondominio } from '../clients/authClient.js';

/**
 * Quem é destinatário de um aviso, por público-alvo.
 *
 * Serve a dois usos que precisam concordar: quem *vê* o aviso e quem entra na
 * conta de "deveria ler". Se divergissem, o relatório cobraria leitura de gente
 * que nunca recebeu o comunicado.
 */
const ALVOS = {
  TODOS: null, // qualquer destinatário
  MORADORES: ['MORADOR', 'DONO_ALUGUEL'],
  // Funcionário é quem trabalha no condomínio, não quem o administra. O
  // síndico estava aqui e era um erro de modelagem: ele **publica** o
  // comunicado. Contado como destinatário, entrava no denominador do próprio
  // aviso — o relatório dizia "1 de 2" cobrando ciência de quem escreveu.
  FUNCIONARIOS: ['PORTEIRO'],
};

/**
 * Quem nunca é destinatário: a administração.
 *
 * Ela é a remetente. Nem `TODOS` a alcança — "todos" é todo mundo a quem o
 * comunicado se dirige, e ninguém publica um aviso para si mesmo.
 *
 * A visão da gestão sobre os avisos é outra, e já existe: Comunicados, onde ela
 * vê o que publicou e quem leu.
 */
const REMETENTES = ['ADMIN_GERAL', 'ADMIN_SINDICO'];

/** Se um aviso é endereçado a quem tem este perfil. */
function ehDestinatario(aviso, perfil) {
  if (REMETENTES.includes(perfil)) return false;
  const perfis = ALVOS[aviso?.publicoAlvo] ?? null;
  return perfis === null || perfis.includes(perfil);
}

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/**
 * Registra que o usuário leu um aviso.
 *
 * O aviso vive no `portaria-service`, em outro banco — não há FK que impeça um
 * id inventado. A consulta ao portaria *é* a integridade referencial: sem ela,
 * qualquer UUID entraria na tabela e o relatório do síndico contaria leitura de
 * aviso que não existe, ou de aviso de outro condomínio.
 */
export async function confirmar(escopo, avisoId) {
  if (!UUID.test(String(avisoId ?? ''))) {
    // `avisos.id` é UUID no portaria (`GenerationType.UUID`). Barrar aqui evita
    // que o Postgres responda com erro de sintaxe de tipo.
    return falha(400, 'Identificador de aviso inválido.');
  }

  const r = await buscarAviso(avisoId);

  if (!r.ok) {
    // 404 do portaria é aviso inexistente; qualquer outra coisa é o portaria
    // indisponível, e aí não dá para afirmar que o aviso não existe.
    if (r.status === 404) return falha(404, 'Aviso não encontrado.');
    return falha(503, 'Não foi possível confirmar o aviso agora. Tente de novo.');
  }

  if (r.aviso.condominioId !== escopo.condominioId) {
    return falha(404, 'Aviso não encontrado.');
  }

  if (r.aviso.publicado === false) {
    return falha(409, 'Este aviso ainda não foi publicado.');
  }

  // Confirmar leitura de aviso que não é para o seu perfil sujaria o relatório
  // com uma linha que o denominador não conta — 9 confirmações de 7
  // destinatários.
  if (!ehDestinatario(r.aviso, escopo.perfil)) {
    return falha(403, 'Este comunicado não é endereçado ao seu perfil.');
  }

  const registro = await leituras.confirmar({
    condominioId: escopo.condominioId,
    avisoId,
    usuarioId: escopo.usuarioId,
  });

  return { sucesso: true, leitura: registro };
}

/**
 * Avisos ativos do condomínio marcados com o que este usuário já confirmou.
 *
 * É a tela do morador: a lista vem do portaria, o "já li" vem daqui. Juntar no
 * backend evita a tela fazer duas chamadas e cruzar por conta própria — e
 * evita que cada tela cruze de um jeito.
 */
export async function meusAvisos(escopo) {
  const avisos = await listarAvisosAtivos(escopo.condominioId);
  if (avisos === null) {
    return falha(503, 'Não foi possível carregar os avisos agora.');
  }

  const confirmados = await leituras.avisosConfirmadosPor(escopo.usuarioId, escopo.condominioId);
  const porId = new Map(confirmados.map((c) => [c.avisoId, c.confirmadaEm]));

  // O portaria devolve todos os ativos do condomínio, sem olhar o público-alvo
  // — é o recorte que faltava. Sem ele o morador recebia comunicado interno de
  // funcionário, e o relatório cobrava dele uma ciência que nunca foi
  // endereçada.
  //
  // Para a gestão a lista vem inteira, mas como consulta: ela acompanha o que
  // está publicado sem que nada lhe seja cobrado.
  const ehRemetente = REMETENTES.includes(escopo.perfil);
  const doMeuPublico = ehRemetente
    ? avisos
    : avisos.filter((a) => ehDestinatario(a, escopo.perfil));

  const itens = doMeuPublico.map((a) => ({
    ...a,
    destinatario: !ehRemetente,
    lido: porId.has(a.id),
    lidoEm: porId.get(a.id) ?? null,
  }));

  return {
    sucesso: true,
    avisos: itens,
    // Quem publica não tem aviso pendente de leitura, por definição.
    pendentes: ehRemetente ? 0 : itens.filter((a) => !a.lido).length,
  };
}

/**
 * Relatório de leitura de um aviso, para a gestão.
 *
 * "8 pessoas confirmaram" sozinho não diz nada — 8 de 9 é ótimo, 8 de 80 é
 * problema. Por isso o total vem do auth-api, e não da contagem de linhas
 * desta tabela.
 *
 * Quando o auth-api não responde, o relatório sai **sem** o total em vez de
 * sair com um total errado: uma porcentagem inventada seria pior que ausente.
 */
export async function relatorio(escopo, avisoId, authorization) {
  if (!UUID.test(String(avisoId ?? ''))) {
    return falha(400, 'Identificador de aviso inválido.');
  }

  const r = await buscarAviso(avisoId);
  if (!r.ok) {
    if (r.status === 404) return falha(404, 'Aviso não encontrado.');
    return falha(503, 'Não foi possível carregar o aviso agora.');
  }
  if (r.aviso.condominioId !== escopo.condominioId) {
    return falha(404, 'Aviso não encontrado.');
  }

  const confirmacoes = await leituras.porAviso(avisoId);
  const quandoLeu = new Map(confirmacoes.map((c) => [c.usuarioId, c.confirmadaEm]));

  const usuarios = await listarUsuariosDoCondominio(escopo.condominioId, authorization);

  if (usuarios === null) {
    return {
      sucesso: true,
      aviso: resumoDoAviso(r.aviso),
      total: null,
      confirmados: confirmacoes.length,
      pendentes: null,
      destinatarios: confirmacoes.map((c) => ({
        usuarioId: c.usuarioId,
        nome: null,
        perfil: null,
        unidadeId: null,
        lido: true,
        lidoEm: c.confirmadaEm,
      })),
      observacao: 'Não foi possível listar os destinatários agora; só quem confirmou aparece.',
    };
  }

  const destinatarios = usuarios.filter(
    (u) => u.alcancavel && ehDestinatario(r.aviso, u.perfil),
  );

  const detalhado = destinatarios.map((u) => ({
    usuarioId: u.id,
    nome: u.nome,
    perfil: u.perfil,
    unidadeId: u.unidadeId,
    lido: quandoLeu.has(u.id),
    lidoEm: quandoLeu.get(u.id) ?? null,
  }));

  return {
    sucesso: true,
    aviso: resumoDoAviso(r.aviso),
    total: detalhado.length,
    confirmados: detalhado.filter((d) => d.lido).length,
    pendentes: detalhado.filter((d) => !d.lido).length,
    destinatarios: detalhado,
  };
}

/**
 * Panorama para a gestão: os avisos ativos e quantos confirmaram cada um.
 *
 * Uma consulta agregada em vez de uma por aviso — a tela lista todos de uma
 * vez, e N avisos não podem virar N+1 idas ao banco.
 */
export async function panorama(escopo) {
  const avisos = await listarAvisosAtivos(escopo.condominioId);
  if (avisos === null) return falha(503, 'Não foi possível carregar os avisos agora.');

  const contagens = await leituras.contarPorAvisos(avisos.map((a) => a.id));

  return {
    sucesso: true,
    avisos: avisos.map((a) => ({
      ...resumoDoAviso(a),
      confirmacoes: contagens.get(a.id) ?? 0,
    })),
  };
}

function resumoDoAviso(a) {
  return {
    id: a.id,
    titulo: a.titulo,
    publicoAlvo: a.publicoAlvo,
    dataInicio: a.dataInicio,
    dataFim: a.dataFim,
    publicado: a.publicado,
  };
}

function falha(status, mensagem) {
  return { sucesso: false, status, mensagem };
}
