import express from 'express';
import User from '../models/User.js';
import authMiddleware, { adminMiddleware, gestaoMiddleware } from '../middleware/auth.js';
import { usuarioPublico, normalizarVinculo } from '../utils/usuarioPublico.js';
import { emitirConvite } from '../services/userManagementService.js';
import { desativarUsuario } from '../services/userManagementService.js';
import {
  PERFIS,
  STATUS_USUARIO,
  CONDOMINIO_DEFAULT,
  podeAcessarPortaria,
  podeGerenciarUsuarios,
} from '../constants/perfis.js';
import { Op } from 'sequelize';

const router = express.Router();

router.get('/', authMiddleware, async (req, res) => {
  try {
    if (!podeGerenciarUsuarios(req.userPerfil)) {
      return res.status(403).json({ sucesso: false, mensagem: 'Acesso negado' });
    }

    const usuarios = await User.findAll({ order: [['createdAt', 'DESC']] });
    res.json({ sucesso: true, usuarios: usuarios.map(usuarioPublico) });
  } catch (err) {
    console.error('Erro em /api/users:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao processar a solicitação' });
  }
});

router.post('/', authMiddleware, gestaoMiddleware, async (req, res) => {
  try {
    const { email, perfil, unidadeId, nomePrecadastro, cpfPrecadastro } = req.body;

    if (!email || !perfil) {
      return res.status(400).json({ sucesso: false, mensagem: 'E-mail e perfil são obrigatórios.' });
    }

    const resultado = await emitirConvite(req.user, {
      email,
      perfil,
      unidadeId,
      nomePrecadastro,
      cpfPrecadastro,
    });

    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }

    res.status(201).json(resultado);
  } catch (err) {
    console.error('Erro em /api/users:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao processar a solicitação' });
  }
});

router.put('/:id', authMiddleware, adminMiddleware, async (req, res) => {
  try {
    const { nome, email, senha, bloco, apartamento, vaga, perfil } = req.body;

    const usuario = await User.scope('withPassword').findByPk(req.params.id);
    if (!usuario) {
      return res.status(404).json({ sucesso: false, mensagem: 'Usuário não encontrado' });
    }

    if (nome) usuario.nome = nome;
    if (email) usuario.email = email;
    if (senha) usuario.senha = senha;
    if (bloco !== undefined) usuario.bloco = normalizarVinculo(bloco);
    if (apartamento !== undefined) usuario.apartamento = normalizarVinculo(apartamento);
    if (vaga !== undefined) usuario.vaga = normalizarVinculo(vaga);

    await usuario.save();

    res.json({ sucesso: true, usuario: usuarioPublico(usuario) });
  } catch (err) {
    console.error('Erro em /api/users:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao processar a solicitação' });
  }
});

router.delete('/:id', authMiddleware, gestaoMiddleware, async (req, res) => {
  try {
    if (req.params.id === req.userId?.toString()) {
      return res.status(400).json({ sucesso: false, mensagem: 'Não é possível desativar sua própria conta' });
    }

    const resultado = await desativarUsuario(req.user, Number(req.params.id));
    if (!resultado.sucesso) {
      return res.status(resultado.status || 400).json(resultado);
    }

    res.json(resultado);
  } catch (err) {
    console.error('Erro em /api/users:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao processar a solicitação' });
  }
});

/* ------------------------------------------------------------------ *
 * Rotas de apoio ao comunicacao-service (chat, avisos, notificações). *
 *                                                                     *
 * O comunicacao-service guarda apenas o id do autor/destinatário — a  *
 * identidade continua sendo do auth-api. Estas três rotas são o que   *
 * ele precisa para exibir nomes e saber para quem disparar.           *
 * ------------------------------------------------------------------ */

/** Só o Admin Geral enxerga além do próprio condomínio. */
function escopoCondominio(req) {
  const { condominioId } = req.user;
  if (req.userPerfil === PERFIS.ADMIN_GERAL && (!condominioId || condominioId === CONDOMINIO_DEFAULT)) {
    return null;
  }
  return condominioId ?? null;
}

/** Campos que o chat e a lista de leitores precisam — nada além disso. */
function contatoPublico(usuario) {
  return {
    id: String(usuario.id),
    nome: usuario.nome,
    perfil: usuario.getPerfilEfetivo?.() ?? usuario.perfil,
    fotoUrl: usuario.fotoUrl ?? null,
    bloco: usuario.bloco ?? null,
    apartamento: usuario.apartamento ?? null,
  };
}

/**
 * Com quem o usuário pode conversar.
 *
 * O chat é morador↔administração: o morador só recebe síndicos e porteiros,
 * enquanto quem opera o condomínio enxerga todo mundo para poder responder.
 */
router.get('/contatos', authMiddleware, async (req, res) => {
  try {
    const condominioId = escopoCondominio(req);
    const where = { status: STATUS_USUARIO.ACTIVE, id: { [Op.ne]: req.userId } };
    if (condominioId) where.condominioId = condominioId;

    if (!podeAcessarPortaria(req.userPerfil)) {
      where.perfil = { [Op.in]: [PERFIS.ADMIN_SINDICO, PERFIS.ADMIN_GERAL, PERFIS.PORTEIRO] };
    }

    const usuarios = await User.findAll({ where, order: [['nome', 'ASC']] });
    res.json({ sucesso: true, contatos: usuarios.map(contatoPublico) });
  } catch (err) {
    console.error('Erro em /api/users/contatos:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao processar a solicitação' });
  }
});

/**
 * Resolve nomes em lote. Recebe ids, devolve os que existem no mesmo
 * condomínio — assim uma lista de leitores ou de conversas vira uma única
 * chamada em vez de uma por participante.
 */
router.post('/lookup', authMiddleware, async (req, res) => {
  try {
    const { ids } = req.body;
    if (!Array.isArray(ids)) {
      return res.status(400).json({ sucesso: false, mensagem: 'Informe a lista de ids.' });
    }
    if (ids.length === 0) {
      return res.json({ sucesso: true, usuarios: [] });
    }
    if (ids.length > 500) {
      return res.status(400).json({ sucesso: false, mensagem: 'No máximo 500 ids por consulta.' });
    }

    const numericos = ids.map((id) => Number(id)).filter((id) => Number.isInteger(id));
    if (numericos.length === 0) {
      return res.json({ sucesso: true, usuarios: [] });
    }

    const condominioId = escopoCondominio(req);
    const where = { id: { [Op.in]: numericos } };
    if (condominioId) where.condominioId = condominioId;

    const usuarios = await User.findAll({ where });
    res.json({ sucesso: true, usuarios: usuarios.map(contatoPublico) });
  } catch (err) {
    console.error('Erro em /api/users/lookup:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao processar a solicitação' });
  }
});

/** Público-alvo de um aviso → perfis que devem recebê-lo. */
const PERFIS_POR_PUBLICO = {
  TODOS: null,
  MORADORES: [PERFIS.MORADOR, PERFIS.DONO_ALUGUEL],
  FUNCIONARIOS: [PERFIS.PORTEIRO, PERFIS.TERCEIRO],
  SINDICO: [PERFIS.ADMIN_SINDICO, PERFIS.ADMIN_GERAL],
};

/**
 * Quem deve receber um aviso de determinado público-alvo.
 *
 * É a origem do denominador do percentual de leitura: sem ela o síndico vê
 * quantos leram, mas não de quantos.
 */
router.get('/destinatarios', authMiddleware, async (req, res) => {
  try {
    if (!podeAcessarPortaria(req.userPerfil)) {
      return res.status(403).json({ sucesso: false, mensagem: 'Acesso negado' });
    }

    const publicoAlvo = String(req.query.publicoAlvo || 'TODOS').toUpperCase();
    if (!(publicoAlvo in PERFIS_POR_PUBLICO)) {
      return res.status(400).json({ sucesso: false, mensagem: `Público-alvo inválido: ${publicoAlvo}` });
    }

    const condominioId = req.query.condominioId || escopoCondominio(req);
    const where = { status: STATUS_USUARIO.ACTIVE };
    if (condominioId) where.condominioId = condominioId;

    const perfis = PERFIS_POR_PUBLICO[publicoAlvo];
    // Quem não tem login não é destinatário de aviso nem de notificação.
    where.perfil = { [Op.in]: perfis ?? Object.values(PERFIS).filter((p) => p !== PERFIS.CONVIDADO) };

    const usuarios = await User.findAll({ where, order: [['nome', 'ASC']] });
    res.json({ sucesso: true, destinatarios: usuarios.map(contatoPublico) });
  } catch (err) {
    console.error('Erro em /api/users/destinatarios:', err);
    res.status(500).json({ sucesso: false, mensagem: 'Erro ao processar a solicitação' });
  }
});

export default router;
