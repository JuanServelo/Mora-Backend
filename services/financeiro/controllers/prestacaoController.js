import * as service from '../services/prestacaoService.js';

function seErro(r, res) {
  if (!r?.erro) return false;
  res.status(r.status ?? 400).json({ sucesso: false, mensagem: r.mensagem });
  return true;
}

// ── Gestão ──────────────────────────────────────────────────────────────────

export async function obter(req, res) {
  const r = await service.obter(req.escopo.condominioId, req.params.competencia);
  if (seErro(r, res)) return;
  res.json({ sucesso: true, prestacao: r });
}

export async function lancar(req, res) {
  const r = await service.lancar(
    req.escopo.condominioId, req.params.competencia, req.body ?? {}, req.escopo.usuarioId,
  );
  if (seErro(r, res)) return;
  res.status(201).json({ sucesso: true, lancamento: r });
}

export async function excluirLancamento(req, res) {
  const r = await service.excluirLancamento(req.escopo.condominioId, req.params.id);
  if (seErro(r, res)) return;
  res.json({ sucesso: true, ...r });
}

export async function publicar(req, res) {
  const r = await service.publicar(req.escopo.condominioId, req.params.competencia, req.escopo.usuarioId);
  if (seErro(r, res)) return;
  res.json({ sucesso: true, prestacao: r });
}

// ── Morador ─────────────────────────────────────────────────────────────────

export async function publicadas(req, res) {
  const competencias = await service.listarPublicadas(req.escopo.condominioId);
  res.json({ sucesso: true, total: competencias.length, competencias });
}

export async function publicada(req, res) {
  const r = await service.obterPublicada(req.escopo.condominioId, req.params.competencia);
  if (seErro(r, res)) return;
  res.json({ sucesso: true, prestacao: r });
}
