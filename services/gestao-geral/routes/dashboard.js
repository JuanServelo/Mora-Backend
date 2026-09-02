import express from 'express';
import { autenticar, exigirAdminGeral } from '../middleware/auth.js';
import {
  montarDashboard,
  montarResumoCondominio,
  montarPainelCondominio,
} from '../services/dashboardService.js';
import { paraCsv } from '../utils/csv.js';

const router = express.Router();

const PERFIS_GESTAO = ['ADMIN_GERAL', 'ADMIN_SINDICO'];

router.use(autenticar);

/** Responde o erro que o service devolveu, quando houver. */
function seErro(resultado, res) {
  if (!resultado?.erro) return false;
  res.status(resultado.status).json({ sucesso: false, mensagem: resultado.mensagem });
  return true;
}

/**
 * Resolve o condomínio da requisição. O Admin Geral escolhe qual; os demais
 * ficam presos ao próprio, ainda que passem outro na URL.
 */
function condominioDoAtor(req) {
  if (req.perfil === 'ADMIN_GERAL') {
    return req.params.id ?? req.query.condominioId ?? null;
  }
  return req.claims.condominioId ?? null;
}

// ── Plataforma: exclusivo do Admin Geral ────────────────────────────────────

router.get('/dashboard', exigirAdminGeral, async (req, res) => {
  const r = await montarDashboard(req.authorization);
  if (seErro(r, res)) return;
  res.json(r);
});

/** Exportação em CSV dos indicadores da plataforma. */
router.get('/dashboard/exportar', exigirAdminGeral, async (req, res) => {
  const r = await montarDashboard(req.authorization);
  if (seErro(r, res)) return;

  const linhas = [
    ...r.condominios.criadosPorMes.map((m) => ({
      secao: 'condominios_por_mes', chave: m.mes, valor: m.total,
    })),
    ...Object.entries(r.usuarios.porPerfil).map(([k, v]) => ({
      secao: 'usuarios_por_perfil', chave: k, valor: v,
    })),
    ...Object.entries(r.usuarios.porStatus).map(([k, v]) => ({
      secao: 'usuarios_por_status', chave: k, valor: v,
    })),
    ...r.usuarios.porCondominio.map((c) => ({
      secao: 'usuarios_por_condominio', chave: c.nome, valor: c.total,
    })),
    ...Object.entries(r.ocorrencias.porStatus).map(([k, v]) => ({
      secao: 'ocorrencias_por_status', chave: k, valor: v,
    })),
  ];

  const data = new Date().toISOString().slice(0, 10);
  res.setHeader('Content-Type', 'text/csv; charset=utf-8');
  res.setHeader('Content-Disposition', `attachment; filename="mora-plataforma-${data}.csv"`);
  res.send(paraCsv(linhas, ['secao', 'chave', 'valor']));
});

// ── Condomínio: Admin Geral vê qualquer um; a gestão vê o próprio ───────────

router.get('/condominios/:id/resumo', async (req, res) => {
  const r = await montarResumoCondominio(req.params.id, req.authorization);
  if (seErro(r, res)) return;
  res.json(r);
});

/** Painel do síndico, com os números do condomínio dele. */
router.get('/painel', async (req, res) => {
  if (!PERFIS_GESTAO.includes(req.perfil)) {
    return res.status(403).json({
      sucesso: false,
      mensagem: 'Apenas perfis de gestão acessam o painel do condomínio.',
    });
  }

  const condominioId = condominioDoAtor(req);
  if (!condominioId) {
    return res.status(400).json({
      sucesso: false,
      mensagem: 'Informe o condomínio em ?condominioId=.',
    });
  }

  const r = await montarPainelCondominio(condominioId, req.authorization);
  if (seErro(r, res)) return;
  res.json(r);
});

export default router;
