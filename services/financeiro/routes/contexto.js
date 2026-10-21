import express from 'express';
import { autenticar } from '../middleware/auth.js';
import { resolverEscopo, resolverUnidade } from '../middleware/escopo.js';
import { protegerRotas } from '../utils/assincrono.js';

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

// Erro num handler async vira 500 com mensagem genérica, em vez de deixar a
// requisição sem resposta e derrubar o processo (Express 4).
export default protegerRotas(router);
