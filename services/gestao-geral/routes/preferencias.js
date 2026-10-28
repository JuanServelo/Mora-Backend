import express from 'express';
import { consultar } from '../config/database.js';
import { autenticar, exigirSessaoValida } from '../middleware/auth.js';

/**
 * Preferências de tela do próprio usuário: `/api/gestao/preferencias/:chave`.
 *
 * Hoje guardam como cada um monta os painéis (Painel Geral e Painel do
 * Condomínio). O conteúdo é da tela; aqui só se garante que a chave é uma das
 * conhecidas, que o valor é um objeto pequeno e que cada um só alcança o seu.
 *
 * Duas barreiras antes do banco: a assinatura do token (`autenticar`) e a
 * sessão confirmada no auth-api (`exigirSessaoValida`), que também fixa o id.
 */
const router = express.Router();

/** `painel.geral`, `painel.condominio.operacional`... */
export const CHAVE_VALIDA = /^painel(\.[a-z0-9-]{1,30}){1,3}$/;

/** Uma configuração de painel cabe com folga; mais que isso não é preferência. */
export const TAMANHO_MAXIMO = 8 * 1024;

/** Mensagem de erro, ou `null` se a preferência pode ser gravada. */
export function validarPreferencia(chave, valor) {
  if (!CHAVE_VALIDA.test(String(chave ?? ''))) return 'Chave de preferência desconhecida.';
  if (valor === null || typeof valor !== 'object' || Array.isArray(valor)) {
    return 'A preferência precisa ser um objeto.';
  }
  if (Buffer.byteLength(JSON.stringify(valor), 'utf8') > TAMANHO_MAXIMO) {
    return 'Preferência grande demais.';
  }
  return null;
}

/** Recusa a chave inválida antes de gastar a ida ao auth-api. */
function chaveConhecida(req, res, next) {
  if (!CHAVE_VALIDA.test(req.params.chave)) {
    return res.status(400).json({ sucesso: false, mensagem: 'Chave de preferência desconhecida.' });
  }
  next();
}

const protegida = [autenticar, chaveConhecida, exigirSessaoValida];

/** Erro de banco vira 500 com mensagem genérica; o detalhe vai para o log. */
const assincrono = (fn) => (req, res) => fn(req, res).catch((err) => {
  console.error('[gestao-geral] erro nas preferências:', err.message);
  res.status(500).json({ sucesso: false, mensagem: 'Erro ao acessar as preferências.' });
});

/** `valor: null` quando o usuário nunca personalizou — a tela usa o padrão. */
router.get('/:chave', protegida, assincrono(async (req, res) => {
  const { chave } = req.params;
  const { rows: [linha] } = await consultar(
    'SELECT valor, atualizado_em FROM preferencias_usuario WHERE usuario_id = $1 AND chave = $2',
    [req.usuarioId, chave],
  );
  res.json({ sucesso: true, chave, valor: linha?.valor ?? null, atualizadoEm: linha?.atualizado_em ?? null });
}));

router.put('/:chave', protegida, assincrono(async (req, res) => {
  const { chave } = req.params;
  const { valor } = req.body ?? {};
  const erro = validarPreferencia(chave, valor);
  if (erro) return res.status(400).json({ sucesso: false, mensagem: erro });

  await consultar(`
    INSERT INTO preferencias_usuario (usuario_id, chave, valor, atualizado_em)
    VALUES ($1, $2, $3::jsonb, NOW())
    ON CONFLICT (usuario_id, chave) DO UPDATE SET valor = EXCLUDED.valor, atualizado_em = NOW()
  `, [req.usuarioId, chave, JSON.stringify(valor)]);

  res.json({ sucesso: true, chave, valor });
}));

/** Volta ao padrão da tela. */
router.delete('/:chave', protegida, assincrono(async (req, res) => {
  const { chave } = req.params;
  await consultar('DELETE FROM preferencias_usuario WHERE usuario_id = $1 AND chave = $2', [req.usuarioId, chave]);
  res.json({ sucesso: true, chave, valor: null });
}));

export default router;
