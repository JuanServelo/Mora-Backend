/** Endereços dos serviços consultados. Em Docker resolvem pelo nome do container. */
export const SERVICOS = {
  auth: process.env.AUTH_API_URL || 'http://localhost:3001',
  portaria: process.env.PORTARIA_SERVICE_URL || 'http://localhost:8090',
  plan: process.env.PLAN_SERVICE_URL || 'http://localhost:8093',
  // Caixa de entrada do usuário. O financeiro publica, não guarda.
  comunicacao: process.env.COMUNICACAO_SERVICE_URL || 'http://localhost:8094',
};

/** Uma fonte lenta não pode segurar o fechamento inteiro. */
export const TIMEOUT_MS = Number(process.env.FONTE_TIMEOUT_MS || 3000);

export const PORT = Number(process.env.PORT || 3004);

/**
 * Credencial que este serviço apresenta ao comunicacao para publicar
 * notificação.
 *
 * O fechamento roda por job, sem usuário logado, e um JWT sintético não passa —
 * o auth-api busca o usuário no banco. Sem o segredo, a notificação deixa de
 * ser publicada e o resto do fechamento segue normal.
 */
export const TOKEN_SERVICO = process.env.SERVICO_TOKEN || null;

export const ehProducao = () => process.env.NODE_ENV === 'production';
