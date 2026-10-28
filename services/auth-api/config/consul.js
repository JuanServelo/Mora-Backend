import Consul from 'consul';

const NOME = 'auth-api';

/**
 * Prefixos que o gateway encaminha para cá.
 *
 * O controle de acesso usa /api/acessos. /api/portaria pertence exclusivamente
 * ao serviço Java; assim os dois serviços não disputam a mesma regra.
 */
const PREFIXOS = [
  '/api/auth',
  '/api/acessos',
  '/api/interno',
  '/api/health',
  '/api/invites',
  '/api/user-management',
  '/api/users',
  '/api/condominios',
  '/api/perfis',
  '/api/estatisticas',
  '/uploads/avatars',
];

/** Regra do Traefik que casa qualquer um dos prefixos. */
export const regraDoGateway = () => PREFIXOS.map((p) => `PathPrefix(\`${p}\`)`).join(' || ');

/**
 * Registra o serviço no Consul, com as tags que o Traefik consome.
 *
 * Mesmo desenho do gestao-geral e do financeiro. Sem registro no Consul,
 * o gateway não consegue encaminhar chamadas para esta instância.
 */
export async function registrarNoConsul(porta) {
  const host = process.env.CONSUL_HOST;
  if (!host) {
    console.log('CONSUL_HOST não definido — registro no Consul desativado');
    return null;
  }

  const port = Number(porta);
  const consul = new Consul({ host, port: process.env.CONSUL_PORT || 8500 });
  const id = process.env.SERVICE_ID || NOME;
  const endereco = process.env.SERVICE_HOST || NOME;

  const registro = {
    id,
    name: NOME,
    address: endereco,
    port,
    tags: [
      'traefik.enable=true',
      `traefik.http.routers.auth.rule=${regraDoGateway()}`,
      'traefik.http.routers.auth.entrypoints=web',
    ],
    check: {
      http: `http://${endereco}:${port}/api/health`,
      interval: '15s',
      timeout: '3s',
      deregistercriticalserviceafter: '1m',
    },
  };

  try {
    await consul.agent.service.register(registro);
    console.log(`  consul: registrado como ${id}`);

    // Sai do catálogo ao encerrar, para não deixar instância morta anunciada.
    const sair = async () => {
      try {
        await consul.agent.service.deregister(id);
      } catch { /* encerrando de qualquer forma */ }
      process.exit(0);
    };
    process.on('SIGTERM', sair);
    process.on('SIGINT', sair);

    return consul;
  } catch (err) {
    console.warn(`  consul: registro falhou (${err.message}) — seguindo sem descoberta`);
    return null;
  }
}
