import express from 'express';
import { autenticar } from '../middleware/auth.js';
import { resolverEscopo } from '../middleware/escopo.js';
import * as servico from '../services/notificacoesService.js';
import { contarConversasNaoLidas } from '../services/conversasService.js';

const router = express.Router();

/**
 * Caixa de entrada do próprio usuário.
 *
 * Não há filtro por condomínio nem por perfil: a notificação é de uma pessoa, e
 * o `usuario_id` da claim é o escopo inteiro. Aceitar um `usuarioId` de query
 * aqui seria abrir a caixa de qualquer um.
 */
router.get('/notificacoes', autenticar, async (req, res) => {
  const limite = Math.min(Math.max(Number(req.query.limite) || 50, 1), 200);

  const resultado = await servico.caixaDeEntrada(req.claims.id, {
    apenasNaoLidas: req.query.naoLidas === 'true',
    limite,
    antesDe: req.query.antesDe || null,
  });

  res.json({ sucesso: true, ...resultado });
});

/**
 * Contador do sino.
 *
 * Soma duas coisas que o usuário vê como uma: notificação não lida e conversa
 * com mensagem nova. Precisam vir juntas porque a primeira mensagem de um
 * morador para a administração não gera notificação — não há participante da
 * gestão a quem endereçar — e sem esta soma o síndico não veria nada acender.
 */
router.get('/notificacoes/resumo', autenticar, resolverEscopo, async (req, res) => {
  const [naoLidas, conversas] = await Promise.all([
    servico.contarNaoLidas(req.claims.id),
    contarConversasNaoLidas(req.escopo),
  ]);

  res.json({ sucesso: true, naoLidas, conversasNaoLidas: conversas, total: naoLidas + conversas });
});

router.patch('/notificacoes/:id/lida', autenticar, async (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id)) {
    return res.status(400).json({ sucesso: false, mensagem: 'Identificador inválido.' });
  }

  const notificacao = await servico.marcarLida(id, req.claims.id);

  // 404 também quando a notificação é de outra pessoa: o filtro por usuário
  // está no WHERE, e distinguir os casos contaria que ela existe.
  if (!notificacao) {
    return res.status(404).json({ sucesso: false, mensagem: 'Notificação não encontrada.' });
  }

  res.json({ sucesso: true, notificacao });
});

router.post('/notificacoes/lidas', autenticar, async (req, res) => {
  const marcadas = await servico.marcarTodasLidas(req.claims.id);
  res.json({ sucesso: true, marcadas });
});

export default router;
