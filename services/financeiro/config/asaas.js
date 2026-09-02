/**
 * Credenciais do gateway de pagamento.
 *
 * A chave nunca entra no código nem no compose versionado: vem do ambiente.
 * Sem ela o serviço sobe normalmente — só as rotas de cobrança ficam
 * indisponíveis, e dizem isso em vez de estourar em runtime.
 */
export const ASAAS = {
  baseUrl: process.env.ASAAS_BASE_URL || 'https://api-sandbox.asaas.com/v3',
  apiKey: process.env.ASAAS_API_KEY || null,
  // O Asaas assina o webhook com um token acordado no cadastro da integração.
  webhookToken: process.env.ASAAS_WEBHOOK_TOKEN || null,
  timeoutMs: Number(process.env.ASAAS_TIMEOUT_MS || 8000),
};

export const gatewayConfigurado = () => Boolean(ASAAS.apiKey);
