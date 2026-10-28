import express from 'express';
import { autenticar } from '../middleware/auth.js';
import { obterCotacaoDolar } from '../services/cambioService.js';

/**
 * `/api/gestao/cambio`: cotação do dólar para a tela em inglês.
 *
 * Qualquer perfil logado, porque morador e síndico veem valores. Basta a
 * assinatura do token: a resposta é a mesma para todos e não tem dado de
 * ninguém.
 */
const router = express.Router();

router.get('/', autenticar, async (_req, res) => {
  const cotacao = await obterCotacaoDolar();
  if (cotacao.erro) {
    return res.status(cotacao.status).json({ sucesso: false, mensagem: cotacao.mensagem });
  }
  res.json({ sucesso: true, moeda: 'USD', ...cotacao, fonte: 'Banco Central (PTAX)' });
});

export default router;
