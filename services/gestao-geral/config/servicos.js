/** Endereços dos serviços consultados. Em Docker resolvem pelo nome do container. */
export const SERVICOS = {
  auth: process.env.AUTH_API_URL || 'http://localhost:3001',
  portaria: process.env.PORTARIA_SERVICE_URL || 'http://localhost:8090',
  plan: process.env.PLAN_SERVICE_URL || 'http://localhost:8093',
};

/** Uma fonte lenta não pode segurar o dashboard inteiro. */
export const TIMEOUT_MS = Number(process.env.FONTE_TIMEOUT_MS || 3000);

export const PORT = Number(process.env.PORT || 3002);

export const ehProducao = () => process.env.NODE_ENV === 'production';
