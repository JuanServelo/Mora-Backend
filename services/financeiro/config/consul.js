import Consul from 'consul';
import { PORT } from './servicos.js';

const NOME = 'financeiro';

/**
 * Registra o serviço no Consul, com as tags que o Traefik consome.
 *
 * Mesmo desenho do `gestao-geral`: em Node o registro é manual, porque não há
 * o equivalente ao spring-cloud-starter-consul-discovery dos serviços Java.
 *
 * Falha de registro não derruba o serviço: sem Consul ele continua acessível
 * pela porta publicada, que é como o frontend o alcança hoje.
 */
export async function registrarNoConsul() {
  const host = process.env.CONSUL_HOST;
  if (!host) {
    console.log('CONSUL_HOST não definido — registro no Consul desativado');
    return null;
  }

  const consul = new Consul({ host, port: process.env.CONSUL_PORT || 8500 });
  const id = `${NOME}-${process.env.HOSTNAME || PORT}`;
  const endereco = process.env.SERVICE_HOST || NOME;

  const registro = {
    id,
    name: NOME,
    address: endereco,
    port: PORT,
    tags: [
      'traefik.enable=true',
      'traefik.http.routers.financeiro.rule=PathPrefix(`/api/financeiro`)',
      'traefik.http.routers.financeiro.entrypoints=web',
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
