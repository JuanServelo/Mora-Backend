/** Endereços dos serviços consultados. Em Docker resolvem pelo nome do container. */
export const SERVICOS = {
  auth: process.env.AUTH_API_URL || 'http://localhost:3001',
  // Os avisos continuam no portaria; só o registro de leitura nasce aqui.
  portaria: process.env.PORTARIA_SERVICE_URL || 'http://localhost:8090',
};

/** Uma fonte lenta não pode segurar a caixa de entrada. */
export const TIMEOUT_MS = Number(process.env.FONTE_TIMEOUT_MS || 3000);

export const PORT = Number(process.env.PORT || 3003);

export const ehProducao = () => process.env.NODE_ENV === 'production';

/**
 * Segredo que autentica outro serviço publicando notificação.
 *
 * O `financeiro` precisa notificar o morador quando a fatura sai, e nesse
 * momento não há usuário logado — o fechamento roda por job. Um JWT de usuário
 * não serve, e forjar um não passa: o auth-api busca o usuário no banco.
 *
 * Sem o segredo definido, a rota interna fica fechada em vez de aberta.
 */
export const TOKEN_SERVICO = process.env.SERVICO_TOKEN || null;
