import Consul from 'consul';

const NOME = 'auth-api';

/**
 * Prefixos que o gateway encaminha para cá.
 *
 * `/api/portaria` fica de fora de propósito: o auth-api também monta rotas
 * nesse prefixo (registro de acesso), mas no Traefik ele pertence ao
 * portaria-service. Anunciar os dois faria o gateway escolher um deles por
 * prioridade de regra, e metade das chamadas cairia no serviço errado. Essas
 * rotas continuam acessíveis pela porta publicada do auth-api.
 */
const PREFIXOS = [
  '/api/auth',
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
 * Mesmo desenho do gestao-geral e do financeiro. Falha de registro não derruba
 * o serviço: sem Consul ele continua acessível pela porta publicada, que é como
 * o frontend o alcança hoje.
 */
export async function registrarNoConsul(porta) {
  const host = process.env.CONSUL_HOST;
  if (!host) {
    console.log('CONSUL_HOST não definido — registro no Consul desativado');
    return null;
  }

  const port = Number(porta);
  const consul = new Consul({ host, port: process.env.CONSUL_PORT || 8500 });
  const id = `${NOME}-${process.env.HOSTNAME || port}`;
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
