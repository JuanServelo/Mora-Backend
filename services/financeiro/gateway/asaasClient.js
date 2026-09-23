import { ASAAS, gatewayConfigurado } from '../config/asaas.js';
import { paraDecimalString } from '../utils/dinheiro.js';

/**
 * Cliente da API do Asaas.
 *
 * Duas coisas que fogem do usual e valem estar escritas:
 *
 * 1. A autenticação **não** é `Authorization: Bearer`. A chave vai num header
 *    próprio, `access_token`. Verificado contra a API de sandbox.
 * 2. Valores viajam como decimal em string ("249.90"), enquanto aqui dentro
 *    tudo é BIGINT em centavos. A conversão acontece só nesta borda.
 *
 * Devolve `{ ok, dados, erro }` em vez de lançar, no mesmo desenho do
 * `utils/httpClient.js`: quem chama decide o que fazer com a falha, e uma
 * indisponibilidade do gateway não pode derrubar o fechamento inteiro.
 */

// Cartão exigiria guardar bandeira e últimos dígitos, e o RNF-14 diz que dado
// de pagamento não é armazenado. Fechar a porta aqui é mais barato que confiar
// em ninguém tentar depois.
const FORMAS_ACEITAS = ['PIX', 'BOLETO'];

async function chamar(metodo, caminho, corpo) {
  if (!gatewayConfigurado()) {
    return { ok: false, erro: 'gateway-nao-configurado', status: 503 };
  }

  try {
    const resposta = await fetch(`${ASAAS.baseUrl}${caminho}`, {
      method: metodo,
      headers: {
        // Nunca logar este header.
        access_token: ASAAS.apiKey,
        Accept: 'application/json',
        ...(corpo && { 'Content-Type': 'application/json' }),
      },
      ...(corpo && { body: JSON.stringify(corpo) }),
      signal: AbortSignal.timeout(ASAAS.timeoutMs),
    });

    const dados = await resposta.json().catch(() => null);

    if (!resposta.ok) {
      // O Asaas devolve { errors: [{ code, description }] }.
      const detalhe = dados?.errors?.map((e) => e.description).join('; ');
      console.warn(`[financeiro] Asaas ${metodo} ${caminho}: HTTP ${resposta.status}`);
      return { ok: false, erro: detalhe || `HTTP ${resposta.status}`, status: resposta.status };
    }

    return { ok: true, dados };
  } catch (err) {
    const motivo = err.name === 'TimeoutError' ? 'timeout' : err.message;
    console.warn(`[financeiro] Asaas ${metodo} ${caminho}: ${motivo}`);
    return { ok: false, erro: motivo };
  }
}

/**
 * Garante o pagador no Asaas, procurando pelo CPF antes de criar.
 *
 * Criar sem procurar duplicaria o cliente a cada fatura, e o gateway passaria a
 * ter um cadastro por mês para o mesmo morador.
 */
export async function garantirCliente({ nome, cpf, email, usuarioId }) {
  const busca = await chamar('GET', `/customers?cpfCnpj=${encodeURIComponent(cpf)}`);
  if (busca.ok && busca.dados?.data?.length) {
    return { ok: true, dados: busca.dados.data[0] };
  }
  return chamar('POST', '/customers', {
    name: nome,
    cpfCnpj: cpf,
    email,
    externalReference: String(usuarioId),
  });
}

/**
 * Cria a cobrança de uma fatura.
 *
 * `externalReference` recebe o id da fatura, e é o que amarra o webhook de
 * volta quando a notificação chega antes de a cobrança estar gravada aqui.
 */
export async function criarCobranca({
  clienteId, forma, valorCentavos, vencimento, descricao, faturaId,
}) {
  if (!FORMAS_ACEITAS.includes(forma)) {
    return { ok: false, erro: `Forma de pagamento não aceita: ${forma}`, status: 400 };
  }
  return chamar('POST', '/payments', {
    customer: clienteId,
    billingType: forma,
    value: Number(paraDecimalString(valorCentavos)),
    dueDate: vencimento,
    description: descricao,
    externalReference: faturaId,
  });
}

export const buscarPixQrCode = (cobrancaId) => chamar('GET', `/payments/${cobrancaId}/pixQrCode`);

export const buscarLinhaDigitavel = (cobrancaId) =>
  chamar('GET', `/payments/${cobrancaId}/identificationField`);

export const consultarCobranca = (cobrancaId) => chamar('GET', `/payments/${cobrancaId}`);

/**
 * Cancela a cobrança no gateway, com uma retentativa.
 *
 * O `DELETE` de boleto devolve HTTP 500 na primeira chamada de forma
 * reproduzível — observado nas duas execuções do smoke, sempre no boleto e
 * nunca no PIX. A segunda tentativa passa. Sem o retry, uma fatura cancelada
 * aqui deixaria a cobrança viva lá, e o morador pagaria o que já não existe.
 */
export async function cancelarCobranca(cobrancaId) {
  const primeira = await chamar('DELETE', `/payments/${cobrancaId}`);
  if (primeira.ok) return primeira;

  // Só insiste em erro do servidor: 400 e 404 são resposta, não falha.
  if (primeira.status && primeira.status < 500) return primeira;

  await new Promise((r) => setTimeout(r, 500));
  return chamar('DELETE', `/payments/${cobrancaId}`);
}
