import express from 'express';
import crypto from 'crypto';
import { Op } from 'sequelize';
import User from '../models/User.js';
import { PERFIS, STATUS_USUARIO } from '../constants/perfis.js';

/**
 * Consultas que outro serviço faz sem usuário logado.
 *
 * O caso que motiva a rota: quando um morador solicita uma reserva que depende
 * de aprovação, o portaria-service precisa saber quem são os síndicos para
 * avisá-los — e o token que ele tem em mãos é o do morador, que não pode
 * listar a equipe do condomínio. Repassar esse token daria 403; fabricar um de
 * síndico seria pior.
 *
 * A credencial aqui é o segredo compartilhado `SERVICO_TOKEN`, o mesmo
 * mecanismo que o comunicacao usa na rota `/interno/notificacoes`.
 *
 * Fica fora do `authMiddleware`: a autenticação é feita aqui dentro.
 */
const router = express.Router();

const TOKEN_SERVICO = process.env.SERVICO_TOKEN || '';

function tokenConfere(recebido) {
  if (!recebido) return false;
  const a = Buffer.from(recebido, 'utf8');
  const b = Buffer.from(TOKEN_SERVICO, 'utf8');
  // Comparação em tempo constante; comprimentos diferentes já não conferem.
  if (a.length !== b.length) return false;
  return crypto.timingSafeEqual(a, b);
}

router.use((req, res, next) => {
  // Sem token configurado a rota fica FECHADA, não aberta: uma variável
  // esquecida no deploy não pode virar uma porta para listar usuários.
  if (!TOKEN_SERVICO) {
    return res.status(503).json({
      sucesso: false,
      mensagem: 'Consulta entre serviços não configurada.',
    });
  }
  if (!tokenConfere(req.headers['x-servico-token'])) {
    return res.status(401).json({ sucesso: false, mensagem: 'Token de serviço inválido.' });
  }
  return next();
});

/**
 * Síndicos ativos de um condomínio.
 *
 * Devolve só id, nome e e-mail: quem chama precisa saber para quem publicar a
 * notificação, não o cadastro completo da pessoa.
 */
router.get('/sindicos', async (req, res) => {
  try {
    const { condominioId } = req.query;
    if (!condominioId) {
      return res.status(400).json({
        sucesso: false,
        mensagem: 'Informe o condominioId.',
      });
    }

    const sindicos = await User.findAll({
      where: {
        condominioId,
        perfil: PERFIS.ADMIN_SINDICO,
        status: { [Op.ne]: STATUS_USUARIO.INACTIVE },
      },
      attributes: ['id', 'nome', 'email', 'perfil'],
      order: [['nome', 'ASC']],
    });

    res.json({
      sucesso: true,
      sindicos: sindicos.map((s) => ({
        id: String(s.id),
        nome: s.nome,
        email: s.email,
        perfil: s.perfil,
      })),
    });
  } catch (err) {
    console.error('[interno] Erro ao listar síndicos:', err.message);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao consultar síndicos.' });
  }
});

export default router;
