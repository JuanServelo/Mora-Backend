import * as notificacoes from '../models/notificacoesModel.js';

/** Origens aceitas. Lista fechada para o campo não virar texto livre. */
const ORIGENS = ['comunicacao', 'financeiro', 'portaria', 'gestao', 'plataforma'];

const LIMITE_TITULO = 150;

/**
 * Publica uma notificação na caixa de um usuário.
 *
 * É o único caminho de escrita: tanto a rota interna, usada pelos outros
 * serviços, quanto o chat daqui passam por aqui. Ter um caminho só é o que
 * torna a centralização real — se cada um inserisse direto na tabela, o
 * serviço seria só um banco compartilhado com outro nome.
 */
export async function publicar(entrada) {
  const erro = validar(entrada);
  if (erro) return { sucesso: false, status: 400, mensagem: erro };

  const registro = await notificacoes.inserir({
    condominioId: entrada.condominioId,
    usuarioId: Number(entrada.usuarioId),
    origem: entrada.origem,
    tipo: entrada.tipo,
    titulo: String(entrada.titulo).slice(0, LIMITE_TITULO),
    mensagem: String(entrada.mensagem),
    dados: entrada.dados ?? {},
    chaveUnica: entrada.chaveUnica ?? null,
    criadoEm: entrada.criadoEm ?? null,
    lidaEm: entrada.lidaEm ?? null,
  });

  // `null` aqui é a chave única barrando repetição, não falha. O publicador
  // tentou de novo — um job reprocessando a competência, por exemplo — e a
  // resposta correta é "já está publicada", não erro.
  if (!registro) {
    return { sucesso: true, repetida: true, notificacao: null };
  }

  return { sucesso: true, repetida: false, notificacao: registro };
}

/**
 * Publica a mesma notificação para várias pessoas.
 *
 * Devolve o que conseguiu: uma caixa que falhe não pode impedir as outras de
 * receber. O caso real é aviso publicado para o condomínio inteiro.
 */
export async function publicarParaVarios(usuarioIds, base) {
  const resultados = await Promise.allSettled(
    usuarioIds.map((usuarioId) =>
      publicar({
        ...base,
        usuarioId,
        // A chave precisa variar por destinatário, senão o primeiro publica e
        // os demais colidem — todo mundo menos um ficaria sem a notificação.
        chaveUnica: base.chaveUnica ? `${base.chaveUnica}:${usuarioId}` : null,
      }),
    ),
  );

  const publicadas = resultados.filter((r) => r.status === 'fulfilled' && r.value.sucesso && !r.value.repetida).length;
  const falhas = resultados.filter((r) => r.status === 'rejected').length;

  if (falhas > 0) {
    console.warn(`[comunicacao] ${falhas} de ${usuarioIds.length} notificações falharam`);
  }

  return { publicadas, repetidas: usuarioIds.length - publicadas - falhas, falhas };
}

function validar(e) {
  if (!e || typeof e !== 'object') return 'Corpo inválido.';
  if (!e.condominioId) return 'condominioId é obrigatório.';
  if (!Number.isInteger(Number(e.usuarioId))) return 'usuarioId deve ser um inteiro.';
  if (!ORIGENS.includes(e.origem)) return `origem deve ser uma de: ${ORIGENS.join(', ')}.`;
  if (!e.tipo || String(e.tipo).length > 40) return 'tipo é obrigatório e tem no máximo 40 caracteres.';
  if (!e.titulo || !String(e.titulo).trim()) return 'titulo é obrigatório.';
  if (!e.mensagem || !String(e.mensagem).trim()) return 'mensagem é obrigatória.';
  if (e.dados !== undefined && (typeof e.dados !== 'object' || e.dados === null || Array.isArray(e.dados))) {
    return 'dados deve ser um objeto.';
  }
  // Datas explícitas existem para o publicador registrar um evento que já
  // aconteceu. Uma string inválida viraria erro de sintaxe do Postgres, que sai
  // como 500 em vez de 400.
  for (const campo of ['criadoEm', 'lidaEm']) {
    if (e[campo] != null && Number.isNaN(Date.parse(e[campo]))) {
      return `${campo} não é uma data válida.`;
    }
  }
  return null;
}

export async function caixaDeEntrada(usuarioId, opcoes) {
  const [itens, naoLidas] = await Promise.all([
    notificacoes.listarDoUsuario(usuarioId, opcoes),
    notificacoes.contarNaoLidas(usuarioId),
  ]);
  return { notificacoes: itens, naoLidas };
}

export const marcarLida = notificacoes.marcarLida;
export const marcarTodasLidas = notificacoes.marcarTodasLidas;
export const contarNaoLidas = notificacoes.contarNaoLidas;
