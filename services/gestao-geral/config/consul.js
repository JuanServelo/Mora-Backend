import Consul from 'consul';
import { PORT } from './servicos.js';

const NOME = 'gestao-geral';

/**
 * Registra o serviço no Consul, com as tags que o Traefik consome.
 *
 * Os serviços Java fazem isso pelo spring-cloud-starter-consul-discovery; em
 * Node é manual, assim como nos serviços auth-api e financeiro.
 *
 * Sem registro no Consul, o gateway não consegue alcançar esta instância.
 */
export async function registrarNoConsul() {
  const host = process.env.CONSUL_HOST;
  if (!host) {
    console.log('CONSUL_HOST não definido — registro no Consul desativado');
    return null;
  }

  const consul = new Consul({ host, port: process.env.CONSUL_PORT || 8500 });
  const id = process.env.SERVICE_ID || NOME;
  const endereco = process.env.SERVICE_HOST || NOME;

  const registro = {
    id,
    name: NOME,
    address: endereco,
    port: PORT,
    tags: [
      'traefik.enable=true',
      'traefik.http.routers.gestao.rule=PathPrefix(`/api/gestao`)',
      'traefik.http.routers.gestao.entrypoints=web',
      // O portal de documentação de todos os serviços mora aqui, em /docs.
      'traefik.http.routers.gestao-docs.rule=PathPrefix(`/docs`)',
      'traefik.http.routers.gestao-docs.entrypoints=web',
    ],
    check: {
      http: `http://${endereco}:${PORT}/health`,
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
