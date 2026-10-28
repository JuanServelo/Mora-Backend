import * as estornoService from '../services/estornoService.js';

function seErro(r, res) {
  if (!r?.erro) return false;
  res.status(r.status ?? 400).json({ sucesso: false, mensagem: r.mensagem });
  return true;
}

/** O síndico pede o estorno de uma fatura paga por PIX. */
export async function estornarFatura(req, res) {
  const r = await estornoService.estornarFatura(
    req.escopo.condominioId, req.params.id, req.body?.motivo, req.claims.id,
  );
  if (seErro(r, res)) return;
  // 202 quando o gateway aceitou mas ainda não devolveu: o pedido segue aberto.
  res.status(r.status === 'CONCLUIDO' ? 200 : 202).json({ sucesso: true, estorno: r });
}

/** Estornos das faturas do condomínio, conciliados com o gateway. */
export async function listar(req, res) {
  const estornos = await estornoService.listarDoCondominio(req.escopo.condominioId);
  res.json({ sucesso: true, total: estornos.length, estornos });
}
