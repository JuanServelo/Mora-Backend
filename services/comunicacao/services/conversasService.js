import { emTransacao, consultar } from '../config/database.js';
import * as conversas from '../models/conversasModel.js';
import * as mensagens from '../models/mensagensModel.js';
import { listarUsuariosDoCondominio, usuarioDoCondominio } from '../clients/authClient.js';
import { listarApartamentos } from '../clients/portariaClient.js';
import { publicar } from './notificacoesService.js';

const TIPOS = ['DIRETA', 'ADMINISTRACAO'];
const LIMITE_CORPO = 4000;

/**
 * Abre uma conversa com a primeira mensagem.
 *
 * Tudo em uma transação: conversa sem mensagem nenhuma apareceria na caixa do
 * destinatário como notificação de algo que não dá para ler.
 */
export async function abrir(escopo, entrada, authorization) {
  const tipo = entrada.tipo ?? (escopo.ehGestao ? 'DIRETA' : 'ADMINISTRACAO');

  if (!TIPOS.includes(tipo)) {
    return falha(400, `tipo deve ser ${TIPOS.join(' ou ')}.`);
  }

  const assunto = String(entrada.assunto ?? '').trim();
  if (!assunto) return falha(400, 'Informe o assunto da conversa.');

  const corpo = String(entrada.corpo ?? '').trim();
  if (!corpo) return falha(400, 'Escreva a primeira mensagem.');
  if (corpo.length > LIMITE_CORPO) return falha(400, `A mensagem passa de ${LIMITE_CORPO} caracteres.`);

  // Quem não é gestão só fala com a administração. Não é restrição de tela: a
  // listagem de usuários que o morador alcança é recortada na unidade dele, e
  // o síndico não está nela — não haveria destinatário a escolher.
  if (tipo === 'DIRETA' && !escopo.ehGestao) {
    return falha(403, 'Converse com a administração do condomínio; ela encaminha a quem for preciso.');
  }

  const participantes = [escopo.usuarioId];

  if (tipo === 'DIRETA') {
    const destinatarioId = Number(entrada.destinatarioId);
    if (!Number.isInteger(destinatarioId)) return falha(400, 'Informe o destinatário.');
    if (destinatarioId === escopo.usuarioId) return falha(400, 'Escolha outra pessoa como destinatário.');

    const destinatario = await usuarioDoCondominio(destinatarioId, escopo.condominioId, authorization);

    // `null` é o auth-api fora do ar, `undefined` é usuário fora do
    // condomínio. Tratar os dois como a mesma coisa transformaria uma falha
    // temporária em "esse usuário não existe", que é mentira.
    if (destinatario === null) {
      return falha(503, 'Não foi possível confirmar o destinatário agora. Tente de novo.');
    }
    if (!destinatario) {
      return falha(404, 'Destinatário não encontrado neste condomínio.');
    }
    if (!destinatario.alcancavel) {
      return falha(400, 'Esse usuário não acessa o sistema e não recebe mensagens.');
    }

    participantes.push(destinatarioId);
  }

  const resultado = await emTransacao(async (cliente) => {
    const conversa = await conversas.criar(cliente, {
      condominioId: escopo.condominioId,
      tipo,
      assunto,
      criadaPor: escopo.usuarioId,
    });

    for (const usuarioId of participantes) {
      await conversas.adicionarParticipante(cliente, conversa.id, usuarioId);
    }

    const mensagem = await mensagens.inserir(cliente, {
      conversaId: conversa.id,
      autorId: escopo.usuarioId,
      autorPerfil: escopo.perfil,
      corpo,
    });

    await mensagens.tocarConversa(cliente, conversa.id, mensagem.criadaEm);

    // Quem escreveu já leu o que escreveu.
    await cliente.query(
      'UPDATE conversa_participantes SET ultima_leitura_em = now() WHERE conversa_id = $1 AND usuario_id = $2',
      [conversa.id, escopo.usuarioId],
    );

    return { conversa: { ...conversa, ultimaMensagemEm: mensagem.criadaEm }, mensagem };
  });

  await notificarOutros(resultado.conversa, resultado.mensagem, escopo, participantes);

  return { sucesso: true, ...resultado };
}

/** Responde numa conversa existente. */
export async function responder(escopo, conversaId, corpoBruto) {
  const corpo = String(corpoBruto ?? '').trim();
  if (!corpo) return falha(400, 'Escreva a mensagem.');
  if (corpo.length > LIMITE_CORPO) return falha(400, `A mensagem passa de ${LIMITE_CORPO} caracteres.`);

  const acesso = await autorizar(escopo, conversaId);
  if (!acesso.sucesso) return acesso;

  const { conversa } = acesso;

  if (conversa.encerradaEm) {
    return falha(409, 'Esta conversa foi encerrada.');
  }

  const resultado = await emTransacao(async (cliente) => {
    // A gestão que responde uma conversa da administração entra como
    // participante agora. Antes disso ela via a conversa pela regra do
    // condomínio, sem linha própria — e sem linha não há marca de leitura.
    await conversas.adicionarParticipante(cliente, conversa.id, escopo.usuarioId);

    const mensagem = await mensagens.inserir(cliente, {
      conversaId: conversa.id,
      autorId: escopo.usuarioId,
      autorPerfil: escopo.perfil,
      corpo,
    });

    await mensagens.tocarConversa(cliente, conversa.id, mensagem.criadaEm);
    await cliente.query(
      'UPDATE conversa_participantes SET ultima_leitura_em = now() WHERE conversa_id = $1 AND usuario_id = $2',
      [conversa.id, escopo.usuarioId],
    );

    return mensagem;
  });

  const participantes = (await conversas.participantesDe(conversa.id)).map((p) => p.usuarioId);
  await notificarOutros(conversa, resultado, escopo, participantes);

  return { sucesso: true, mensagem: resultado };
}

export async function listar(escopo, { incluirEncerradas = false } = {}, authorization) {
  const itens = await conversas.listarVisiveis(
    escopo.usuarioId,
    escopo.condominioId,
    escopo.ehGestao,
    { incluirEncerradas },
  );

  const pessoas = await diretorio(escopo, authorization);

  return {
    sucesso: true,
    conversas: itens.map(({ ultimoCorpo, participantes, ...c }) => ({
      ...c,
      // Com quem é a conversa, do ponto de vista de quem pergunta. Para a
      // gestão é o que a identifica — "Infiltração na garagem" não diz de
      // quem é, e uma fila de chamados sem nome obriga a abrir um por um.
      contraparte: contraparteDe({ ...c, participantes }, escopo, pessoas),
      // O resumo é o começo da última mensagem; a conversa inteira só vem
      // quando alguém abre.
      resumo: ultimoCorpo ? String(ultimoCorpo).slice(0, 120) : null,
    })),
  };
}

/**
 * Mapa `usuarioId -> { nome, fotoUrl, perfil, unidadeId }` do condomínio.
 *
 * Só para a gestão: a listagem que o morador alcança é recortada na unidade
 * dele e não traz a administração. Para ele a contraparte é sempre "a
 * administração", então nome e foto de quem responde não acrescentam nada.
 *
 * Uma chamada por listagem, não uma por conversa. E falha macia: sem o
 * diretório a conversa aparece sem o nome, que é bem melhor que a lista inteira
 * não carregar porque o auth-api está fora do ar.
 */
async function diretorio(escopo, authorization) {
  if (!escopo.ehGestao || !authorization) return new Map();

  // As duas fontes em paralelo: uma não depende da outra, e somar as latências
  // atrasaria a lista sem motivo.
  const [usuarios, apartamentos] = await Promise.all([
    listarUsuariosDoCondominio(escopo.condominioId, authorization),
    listarApartamentos(escopo.condominioId),
  ]);

  if (!usuarios) return new Map();

  // O auth-api guarda só o UUID da unidade; bloco e número vivem no portaria.
  // Se ele não responder, o nome vai sem a unidade — melhor que a lista falhar.
  const unidades = new Map(
    (apartamentos ?? []).map((a) => [
      a.id,
      [a.blocoNome, a.numero ? `Apto ${a.numero}` : null].filter(Boolean).join(' · ') || null,
    ]),
  );

  return new Map(
    usuarios.map((u) => [
      u.id,
      {
        id: u.id,
        nome: u.nome,
        fotoUrl: u.fotoUrl,
        perfil: u.perfil,
        unidadeId: u.unidadeId,
        unidade: u.unidadeId ? unidades.get(u.unidadeId) ?? null : null,
      },
    ]),
  );
}

/**
 * Com quem é a conversa, na perspectiva de quem está olhando.
 *
 * Não é "quem criou": numa conversa direta que a própria gestão abriu, quem
 * criou é ela mesma, e o nome dela como título não identifica nada. O que
 * identifica é o outro lado.
 *
 * A conversa com a administração ainda sem resposta tem um participante só — o
 * morador. Por isso o criador entra como reserva quando a lista de
 * participantes não oferece ninguém além de quem pergunta.
 */
function contraparteDe(conversa, escopo, pessoas) {
  const outros = (conversa.participantes ?? []).filter((id) => id !== escopo.usuarioId);
  const id = outros[0] ?? (conversa.criadaPor !== escopo.usuarioId ? conversa.criadaPor : null);
  return id ? pessoas.get(id) ?? null : null;
}

/** Abre a conversa e marca como lida — abrir é ler. */
export async function detalhar(escopo, conversaId, authorization) {
  const acesso = await autorizar(escopo, conversaId);
  if (!acesso.sucesso) return acesso;

  const lista = await mensagens.listarDaConversa(conversaId);

  // Só tem efeito para quem já é participante; a gestão que apenas espiou a
  // conversa da administração sem responder não vira participante por abrir.
  await conversas.marcarLeitura(conversaId, escopo.usuarioId);

  const pessoas = await diretorio(escopo, authorization);
  const participantes = await conversas.participantesDe(conversaId);

  return {
    sucesso: true,
    conversa: {
      ...acesso.conversa,
      contraparte: contraparteDe(
        { ...acesso.conversa, participantes: participantes.map((p) => p.usuarioId) },
        escopo,
        pessoas,
      ),
    },
    participantes: participantes.map((p) => ({ ...p, pessoa: pessoas.get(p.usuarioId) ?? null })),
    // O nome vai em cada mensagem porque uma conversa com a administração pode
    // ter mais de uma pessoa da gestão respondendo ao longo do tempo — e o
    // `autor_perfil` gravado diz o papel, não quem era.
    mensagens: lista.map((m) => ({ ...m, autor: pessoas.get(m.autorId) ?? null })),
  };
}

export async function encerrar(escopo, conversaId) {
  const acesso = await autorizar(escopo, conversaId);
  if (!acesso.sucesso) return acesso;

  // Encerrar é ato da administração: o morador que abriu não fecha o assunto
  // sozinho, senão a gestão perde a pendência da lista sem ter respondido.
  if (!escopo.ehGestao) {
    return falha(403, 'Somente a administração encerra uma conversa.');
  }

  const conversa = await conversas.encerrar(acesso.conversa.id, escopo.condominioId);
  return conversa ? { sucesso: true, conversa } : falha(404, 'Conversa não encontrada.');
}

export async function removerMensagem(escopo, mensagemId) {
  const id = Number(mensagemId);
  if (!Number.isInteger(id)) return falha(400, 'Identificador de mensagem inválido.');

  const removida = await mensagens.remover(id, escopo.usuarioId);
  if (!removida) {
    // 404 tanto para "não existe" quanto para "não é sua": distinguir contaria
    // a quem tentou que a mensagem de outra pessoa existe.
    return falha(404, 'Mensagem não encontrada.');
  }
  return { sucesso: true, mensagem: removida };
}

/**
 * Conversas do usuário com mensagem não lida — entra no contador do sino.
 *
 * Existe separado do contador de notificações porque a conversa da
 * administração ainda sem resposta não gera notificação para ninguém: não há
 * participante da gestão a quem endereçar. Sem esta contagem, a primeira
 * mensagem de um morador não acenderia nada para o síndico.
 */
export async function contarConversasNaoLidas(escopo) {
  const { rows } = await consultar(
    `SELECT COUNT(*)::int AS total FROM (
       SELECT c.id
         FROM conversas c
         LEFT JOIN conversa_participantes p
                ON p.conversa_id = c.id AND p.usuario_id = $1
        WHERE c.condominio_id = $2
          AND c.encerrada_em IS NULL
          AND (p.usuario_id IS NOT NULL OR ($3::boolean AND c.tipo = 'ADMINISTRACAO'))
          AND EXISTS (
                SELECT 1 FROM mensagens m
                 WHERE m.conversa_id = c.id
                   AND m.removida_em IS NULL
                   AND m.autor_id <> $1
                   AND m.criada_em > COALESCE(p.ultima_leitura_em, c.criada_em - interval '100 years')
              )
     ) AS pendentes`,
    [escopo.usuarioId, escopo.condominioId, escopo.ehGestao],
  );
  return rows[0].total;
}

/* --------------------------------------------------------------- apoio --- */

/**
 * Confere que o usuário pode ver a conversa.
 *
 * Duas portas: ser participante, ou ser gestão do condomínio numa conversa
 * `ADMINISTRACAO`. Qualquer outra combinação é 404, e não 403 — dizer "existe,
 * mas não é sua" já entrega que existe.
 */
async function autorizar(escopo, conversaId) {
  const id = Number(conversaId);
  if (!Number.isInteger(id)) return falha(400, 'Identificador de conversa inválido.');

  const conversa = await conversas.buscarPorId(id);
  if (!conversa) return falha(404, 'Conversa não encontrada.');

  // Vale antes de qualquer outra regra: o escopo de condomínio não é negociável.
  if (conversa.condominioId !== escopo.condominioId) return falha(404, 'Conversa não encontrada.');

  const ehParticipante = await conversas.participa(id, escopo.usuarioId);
  const porRegraDeGestao = escopo.ehGestao && conversa.tipo === 'ADMINISTRACAO';

  if (!ehParticipante && !porRegraDeGestao) return falha(404, 'Conversa não encontrada.');

  return { sucesso: true, conversa, ehParticipante };
}

/**
 * Avisa os outros participantes de que chegou mensagem.
 *
 * Falha aqui não derruba o envio: a mensagem já está gravada, e a conversa a
 * mostra assim que o destinatário abrir. Perder a notificação é incômodo;
 * perder a mensagem por causa dela seria bem pior.
 */
async function notificarOutros(conversa, mensagem, escopo, participantes) {
  const destinos = participantes.filter((id) => id !== escopo.usuarioId);
  if (destinos.length === 0) return;

  const resumo = mensagem.corpo.length > 140 ? `${mensagem.corpo.slice(0, 140)}…` : mensagem.corpo;

  const resultados = await Promise.allSettled(
    destinos.map((usuarioId) =>
      publicar({
        condominioId: conversa.condominioId,
        usuarioId,
        origem: 'comunicacao',
        tipo: 'MENSAGEM_RECEBIDA',
        titulo: conversa.assunto,
        mensagem: resumo,
        dados: { conversaId: conversa.id, mensagemId: mensagem.id },
        // Uma notificação por mensagem, e a mensagem já é única.
        chaveUnica: `msg:${mensagem.id}:${usuarioId}`,
      }),
    ),
  );

  const falhas = resultados.filter((r) => r.status === 'rejected');
  if (falhas.length) {
    console.warn('[comunicacao] falha ao notificar mensagem:', falhas[0].reason?.message);
  }
}

function falha(status, mensagem) {
  return { sucesso: false, status, mensagem };
}
