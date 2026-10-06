import * as service from '../services/multaService.js';

function seErro(r, res) {
  if (!r?.erro) return false;
  res.status(r.status ?? 400).json({ sucesso: false, mensagem: r.mensagem });
  return true;
}

// ── Gestão ──────────────────────────────────────────────────────────────────

export async function listar(req, res) {
  const { status, unidadeId } = req.query;
  const multas = await service.listar(req.escopo.condominioId, { status, unidadeId });
  res.json({ sucesso: true, total: multas.length, multas });
}

export async function aplicar(req, res) {
  const r = await service.aplicar(
    req.escopo.condominioId, req.body ?? {}, req.escopo.usuarioId, req.authorization,
  );
  if (seErro(r, res)) return;
  res.status(201).json({ sucesso: true, multa: r });
}

export async function julgar(req, res) {
  const r = await service.julgar(
    req.escopo.condominioId, req.params.id, req.body ?? {}, req.escopo.usuarioId, req.authorization,
  );
  if (seErro(r, res)) return;
  res.json({ sucesso: true, multa: r });
}

export async function cancelar(req, res) {
  const r = await service.cancelar(
    req.escopo.condominioId, req.params.id, req.body ?? {}, req.escopo.usuarioId,
  );
  if (seErro(r, res)) return;
  res.json({ sucesso: true, multa: r });
}

// ── Unidade ─────────────────────────────────────────────────────────────────

export async function minhas(req, res) {
  const multas = await service.listarDaUnidade(req.escopo.condominioId, req.escopo.unidadeId);
  res.json({ sucesso: true, total: multas.length, multas });
}

export async function recorrer(req, res) {
  const r = await service.recorrer(
    req.escopo.condominioId, req.escopo.unidadeId, req.params.id, req.body ?? {},
  );
  if (seErro(r, res)) return;
  res.json({ sucesso: true, multa: r });
}
