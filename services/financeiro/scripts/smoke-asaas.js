// Prova o contrato do gateway contra o sandbox de verdade: cria pagador, emite
// PIX e boleto, busca os dados de pagamento e cancela o que criou.
//
// Não é teste automatizado — bate na rede e depende de chave válida. Rodar à
// mão quando a integração mudar: `node scripts/smoke-asaas.js`
import 'dotenv/config';
import {
  garantirCliente, criarCobranca, buscarPixQrCode,
  buscarLinhaDigitavel, cancelarCobranca,
} from '../gateway/asaasClient.js';

/** CPF de teste com dígitos verificadores corretos — o Asaas valida. */
function cpfValido(base = '249715637') {
  const dv = (nums) => {
    const peso = nums.length + 1;
    const soma = nums.reduce((a, n, i) => a + n * (peso - i), 0);
    const r = (soma * 10) % 11;
    return r === 10 ? 0 : r;
  };
  const n = base.split('').map(Number);
  const d1 = dv(n);
  const d2 = dv([...n, d1]);
  return base + d1 + d2;
}

const passo = (nome, r, extra = '') => {
  console.log(`${r.ok ? 'ok    ' : 'FALHOU'} ${nome}${extra ? ' — ' + extra : ''}`);
  if (!r.ok) console.log(`       erro: ${r.erro}`);
  return r.ok;
};

const criados = [];

try {
  const cpf = cpfValido();
  const cliente = await garantirCliente({
    nome: 'Morador de Teste', cpf, email: 'morador.teste@qa.mora.local', usuarioId: 999,
  });
  if (!passo('garantirCliente', cliente, cliente.dados?.id)) process.exit(1);

  const pix = await criarCobranca({
    clienteId: cliente.dados.id, forma: 'PIX', valorCentavos: 35000,
    vencimento: '2026-09-15', descricao: 'Taxa condominial 08/2026 — unidade 101',
    faturaId: 'smoke-fatura-pix',
  });
  if (!passo('criarCobranca PIX', pix, `valor devolvido: ${pix.dados?.value}`)) process.exit(1);
  criados.push(pix.dados.id);

  const qr = await buscarPixQrCode(pix.dados.id);
  passo('buscarPixQrCode', qr, qr.ok ? `payload com ${qr.dados?.payload?.length} chars` : '');

  const boleto = await criarCobranca({
    clienteId: cliente.dados.id, forma: 'BOLETO', valorCentavos: 42050,
    vencimento: '2026-09-15', descricao: 'Taxa condominial 08/2026 — unidade 102',
    faturaId: 'smoke-fatura-boleto',
  });
  if (passo('criarCobranca BOLETO', boleto,
      boleto.dados?.bankSlipUrl ? 'bankSlipUrl presente' : 'sem bankSlipUrl')) {
    criados.push(boleto.dados.id);
    const linha = await buscarLinhaDigitavel(boleto.dados.id);
    passo('buscarLinhaDigitavel', linha,
      linha.ok ? `${String(linha.dados?.identificationField).length} dígitos` : '');
  }

  const recusa = await criarCobranca({
    clienteId: cliente.dados.id, forma: 'CREDIT_CARD', valorCentavos: 1000,
    vencimento: '2026-09-15', descricao: 'nao deve passar', faturaId: 'x',
  });
  console.log(`${recusa.ok ? 'FALHOU' : 'ok    '} allowlist recusa CREDIT_CARD`);
} finally {
  for (const id of criados) {
    const r = await cancelarCobranca(id);
    console.log(`${r.ok ? 'ok    ' : 'FALHOU'} limpeza: cancelou ${id}`);
  }
}
