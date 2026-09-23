import { SERVICOS, TOKEN_SERVICO } from '../config/servicos.js';
import * as notificacoesModel from '../models/notificacoesModel.js';

/**
 * Publica a notificação no `comunicacao-service`.
 *
 * As notificações eram gravadas na tabela local. Passaram a viver no
 * comunicacao porque a caixa de entrada é **do usuário**, não do financeiro: o
 * morador tem uma só, e ela mistura fatura, aviso e mensagem. Com uma tabela por
 * serviço, a tela precisaria juntar as fontes e ordenar por conta própria.
 *
 * A assinatura ficou igual de propósito — os quatro pontos que chamam isto não
 * mudaram uma linha.
 *
 * Falhar aqui **não derruba quem chamou**: o fechamento já gravou as faturas, e
 * o morador as vê na tela de cobranças de qualquer forma. Perder a notificação
 * incomoda; perder o fechamento por causa dela seria bem pior.
 */
export async function criar(usuarioId, condominioId, tipo, titulo, mensagem, metadados) {
  if (!usuarioId) return null;

  if (!TOKEN_SERVICO) {
    console.warn('[financeiro] SERVICO_TOKEN ausente — notificação não publicada');
    return null;
  }

  try {
    const resposta = await fetch(`${SERVICOS.comunicacao}/api/comunicacao/interno/notificacoes`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Servico-Token': TOKEN_SERVICO,
      },
      body: JSON.stringify({
        condominioId,
        usuarioId,
        origem: 'financeiro',
        tipo,
        titulo,
        mensagem: mensagem ?? titulo,
        dados: metadados ?? {},
        chaveUnica: chaveDe(tipo, metadados),
      }),
      signal: AbortSignal.timeout(3000),
    });

    if (!resposta.ok) {
      console.warn(`[financeiro] comunicacao recusou a notificação: HTTP ${resposta.status}`);
      return null;
    }

    const corpo = await resposta.json().catch(() => ({}));
    return corpo.notificacao ?? null;
  } catch (err) {
    const motivo = err.name === 'TimeoutError' ? 'timeout' : err.message;
    console.warn(`[financeiro] falha ao publicar notificação: ${motivo}`);
    return null;
  }
}

/**
 * Chave que impede a mesma notificação de entrar duas vezes.
 *
 * Importa em dois cenários reais: o fechamento reprocessado na mesma
 * competência, e `FATURA_VENCIDA`, que pode vir do webhook do gateway **e** do
 * job diário para a mesma fatura. Avisar duas vezes do mesmo atraso é ruído.
 *
 * Sem `faturaId` não há como gerar chave estável, e aí vale publicar sem ela —
 * repetir é melhor que engolir.
 */
function chaveDe(tipo, metadados) {
  const faturaId = metadados?.faturaId;
  return faturaId ? `financeiro:${tipo}:${faturaId}` : null;
}

/* ------------------------------------------------------------------------- */
/* Leitura: só o que sobrou da tabela local.                                  */
/*                                                                            */
/* O frontend passou a ler a caixa de entrada no comunicacao-service. Estas    */
/* funções continuam servindo as rotas antigas, que ainda devolvem os          */
/* registros gravados antes da virada — quem tiver uma aba velha aberta não    */
/* recebe erro. Escrita nova não passa mais por aqui.                          */
/* ------------------------------------------------------------------------- */

export async function listar(usuarioId) {
  return notificacoesModel.listarPorUsuario(usuarioId);
}

export async function naoLidas(usuarioId) {
  return notificacoesModel.contarNaoLidas(usuarioId);
}

export async function marcarLida(id, usuarioId) {
  return notificacoesModel.marcarLida(id, usuarioId);
}

export async function marcarTodasLidas(usuarioId) {
  return notificacoesModel.marcarTodasLidas(usuarioId);
}
