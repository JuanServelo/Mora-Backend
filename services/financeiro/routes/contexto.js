import express from 'express';
import { autenticar } from '../middleware/auth.js';
import { resolverEscopo, resolverUnidade } from '../middleware/escopo.js';

const router = express.Router();

/**
 * Devolve o escopo que o serviço resolveu para quem chamou.
 *
 * Existe para tornar a autorização verificável de fora: dá para conferir que o
 * `condominioId` sai do token e não da query, e que a unidade é resolvida no
 * auth-api, sem precisar de dados de cobrança cadastrados. Não expõe nada que o
 * próprio usuário já não possa ver sobre si.
 */
router.get('/contexto', autenticar, resolverEscopo, resolverUnidade, (req, res) => {
  res.json({
    sucesso: true,
    perfil: req.perfil,
    escopo: req.escopo,
  });
});

export default router;
