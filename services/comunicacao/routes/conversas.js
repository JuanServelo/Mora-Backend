import express from 'express';
import { autenticar, exigirPerfis, PERFIS_COMUNICAVEIS, PERFIS_GESTAO } from '../middleware/auth.js';
import { resolverEscopo, exigirEscrita } from '../middleware/escopo.js';
import * as servico from '../services/conversasService.js';
import { listarUsuariosDoCondominio } from '../clients/authClient.js';

const router = express.Router();

// Convidado não conversa: o acesso dele é de visita, e o histórico de conversa
// sobrevive à visita.
router.use(autenticar, exigirPerfis(...PERFIS_COMUNICAVEIS), resolverEscopo);

router.get('/conversas', async (req, res) => {
  const resultado = await servico.listar(
    req.escopo,
    { incluirEncerradas: req.query.encerradas === 'true' },
    req.authorization,
  );
  res.json(resultado);
});

router.post('/conversas', exigirEscrita, async (req, res) => {
  const resultado = await servico.abrir(req.escopo, req.body ?? {}, req.authorization);
  if (!resultado.sucesso) return res.status(resultado.status).json(resultado);
  res.status(201).json(resultado);
});

router.get('/conversas/:id', async (req, res) => {
  const resultado = await servico.detalhar(req.escopo, req.params.id, req.authorization);
  if (!resultado.sucesso) return res.status(resultado.status).json(resultado);
  res.json(resultado);
});

router.post('/conversas/:id/mensagens', exigirEscrita, async (req, res) => {
  const resultado = await servico.responder(req.escopo, req.params.id, req.body?.corpo);
  if (!resultado.sucesso) return res.status(resultado.status).json(resultado);
  res.status(201).json(resultado);
});

router.patch('/conversas/:id/encerrar', exigirEscrita, async (req, res) => {
  const resultado = await servico.encerrar(req.escopo, req.params.id);
  if (!resultado.sucesso) return res.status(resultado.status).json(resultado);
  res.json(resultado);
});

router.delete('/mensagens/:id', exigirEscrita, async (req, res) => {
  const resultado = await servico.removerMensagem(req.escopo, req.params.id);
  if (!resultado.sucesso) return res.status(resultado.status).json(resultado);
  res.json(resultado);
});

/**
 * Com quem a gestão pode abrir conversa direta.
 *
 * Só para gestão. O morador até alcança a listagem do auth-api, mas recortada
 * na própria unidade — sem a administração e sem os vizinhos. É o mesmo limite
 * que faz a conversa dele ser endereçada à administração, e não a uma pessoa.
 */
router.get('/contatos', exigirPerfis(...PERFIS_GESTAO), async (req, res) => {
  const usuarios = await listarUsuariosDoCondominio(req.escopo.condominioId, req.authorization);

  if (usuarios === null) {
    return res.status(503).json({
      sucesso: false,
      mensagem: 'Não foi possível carregar os contatos agora.',
    });
  }

  res.json({
    sucesso: true,
    // Sem quem não acessa o sistema, e sem o próprio: ninguém abre conversa
    // consigo mesmo, e a lista já é longa o bastante.
    contatos: usuarios.filter((u) => u.alcancavel && u.id !== req.escopo.usuarioId),
  });
});

export default router;
