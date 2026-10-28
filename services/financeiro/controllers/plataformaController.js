import * as plataformaService from '../services/plataformaService.js';

function seErro(r, res) {
  if (!r?.erro) return false;
  res.status(r.status ?? 400).json({ sucesso: false, mensagem: r.mensagem });
  return true;
}

export async function ativar(req, res) {
  const r = await plataformaService.ativar(req.body, req.claims.id, req.authorization);
  if (seErro(r, res)) return;
  res.status(201).json({ sucesso: true, assinatura: r });
}

export async function listar(_req, res) {
  const assinaturas = await plataformaService.listar();
  res.json({ sucesso: true, total: assinaturas.length, assinaturas });
}

export async function cancelar(req, res) {
  const r = await plataformaService.cancelar(req.params.id);
  if (seErro(r, res)) return;
  res.json({ sucesso: true, assinatura: r });
}

export async function estornarPagamento(req, res) {
  const r = await plataformaService.estornarPagamento(req.params.id, req.body?.motivo, req.claims.id);
  if (seErro(r, res)) return;
  res.status(r.status === 'CONCLUIDO' ? 200 : 202).json({ sucesso: true, estorno: r });
}

/** O síndico vê a mensalidade do condomínio dele e o link de cada cobrança. */
export async function minhaAssinatura(req, res) {
  const assinatura = await plataformaService.doCondominio(req.claims.condominioId);
  res.json({ sucesso: true, assinatura });
}
