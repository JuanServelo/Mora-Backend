import express from 'express';
import { timingSafeEqual } from 'node:crypto';
import { TOKEN_SERVICO } from '../config/servicos.js';
import { publicar, publicarParaVarios } from '../services/notificacoesService.js';

const router = express.Router();

/**
 * Autentica outro serviço, não um usuário.
 *
 * O caso que exige isto é o fechamento de competência do `financeiro`: ele roda
 * como job, sem ninguém logado, e precisa avisar o morador de que a fatura
 * saiu. Um JWT de usuário não serve, e forjar um não passa — o `authMiddleware`
 * do auth-api faz `User.findByPk(decoded.id)`, e não existe usuário com o id de
 * um token sintético.
 *
 * Sem `SERVICO_TOKEN` definido, a rota fica **fechada**. O padrão inseguro
 * seria deixá-la aberta quando não configurada, e é exatamente assim que o
 * `AuthFilter` do portaria virou um buraco.
 */
function autenticarServico(req, res, next) {
  if (!TOKEN_SERVICO) {
    return res.status(503).json({
      sucesso: false,
      mensagem: 'Publicação entre serviços não está configurada neste ambiente.',
    });
  }

  const recebido = req.get('X-Servico-Token') ?? '';

  // Comparação de tempo constante: `===` em string vaza o tamanho do prefixo
  // correto pelo tempo de resposta. O custo é uma linha.
  const a = Buffer.from(recebido);
  const b = Buffer.from(TOKEN_SERVICO);
  if (a.length !== b.length || !timingSafeEqual(a, b)) {
    return res.status(401).json({ sucesso: false, mensagem: 'Credencial de serviço inválida.' });
  }

  next();
}

router.use(autenticarServico);

/**
 * Publica uma notificação em nome de outro serviço.
 *
 * `chaveUnica` é do publicador e é o que impede repetição: o fechamento pode
 * rodar duas vezes na mesma competência, e sem ela o morador receberia o mesmo
 * aviso de novo. Repetição responde 200 com `repetida: true` — devolver erro
 * faria o publicador tentar de novo para sempre, pelo mesmo motivo que o
 * webhook do Asaas responde 200 a reentrega.
 */
router.post('/notificacoes', async (req, res) => {
  const destinatarios = req.body?.usuarioIds;

  if (Array.isArray(destinatarios)) {
    if (destinatarios.length === 0) {
      return res.status(400).json({ sucesso: false, mensagem: 'usuarioIds está vazio.' });
    }
    if (destinatarios.length > 500) {
      return res.status(400).json({ sucesso: false, mensagem: 'No máximo 500 destinatários por chamada.' });
    }

    const resumo = await publicarParaVarios(destinatarios, req.body);
    return res.status(202).json({ sucesso: true, ...resumo });
  }

  const resultado = await publicar(req.body);
  if (!resultado.sucesso) {
    return res.status(resultado.status).json(resultado);
  }

  res.status(resultado.repetida ? 200 : 201).json(resultado);
});

export default router;
