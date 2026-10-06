import * as service from '../services/contratoService.js';

function seErro(r, res) {
  if (!r?.erro) return false;
  res.status(r.status ?? 400).json({ sucesso: false, mensagem: r.mensagem });
  return true;
}

// ── Gestão ──────────────────────────────────────────────────────────────────

export async function listar(req, res) {
  const contratos = await service.listar(req.escopo.condominioId, req.authorization);
  res.json({ sucesso: true, total: contratos.length, contratos });
}

export async function criar(req, res) {
  const r = await service.criar(req.escopo.condominioId, req.body ?? {}, {}, req.authorization);
  if (seErro(r, res)) return;
  res.status(201).json({ sucesso: true, contrato: r });
}

export async function encerrar(req, res) {
  const r = await service.encerrar(req.escopo.condominioId, req.params.id);
  if (seErro(r, res)) return;
  res.json({ sucesso: true, contrato: r });
}

// ── Unidade ─────────────────────────────────────────────────────────────────

export async function daUnidade(req, res) {
  const contratos = await service.listarDaUnidade(
    req.escopo.condominioId, req.escopo.unidadeId, req.authorization,
  );
  res.json({ sucesso: true, total: contratos.length, contratos });
}

export async function criarComoDono(req, res) {
  const r = await service.criar(req.escopo.condominioId, req.body ?? {}, {
    donoId: req.escopo.usuarioId,
    unidadeDoDono: req.escopo.unidadeId,
  }, req.authorization);
  if (seErro(r, res)) return;
  res.status(201).json({ sucesso: true, contrato: r });
}

export async function encerrarComoDono(req, res) {
  const r = await service.encerrar(req.escopo.condominioId, req.params.id, {
    unidadeDoDono: req.escopo.unidadeId,
  });
  if (seErro(r, res)) return;
  res.json({ sucesso: true, contrato: r });
}
