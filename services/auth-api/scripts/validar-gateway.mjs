import 'dotenv/config';
import jwt from 'jsonwebtoken';
import { Op } from 'sequelize';
import User from '../models/User.js';
import sequelize from '../config/database.js';

// Read-only integration check. No token, password, email or response body is printed.
const gateway = process.env.GATEWAY_URL || 'http://traefik:8087';
const nomes = ['auth-api', 'gestao-geral', 'financeiro', 'meeting-service', 'plan-service', 'portaria-service', 'comunicacao-service'];
let falhas = 0;
const verificar = (ok, mensagem) => { console.log(`${ok ? 'OK' : 'FALHOU'} ${mensagem}`); if (!ok) falhas++; };

async function obter(caminho, options = {}) {
  return fetch(gateway + caminho, { signal: AbortSignal.timeout(15000), ...options });
}

try {
  const admin = await User.findOne({ where: { perfil: 'ADMIN_GERAL', status: 'active' }, order: [['id', 'ASC']] });
  const sindico = await User.findOne({ where: { perfil: 'ADMIN_SINDICO', status: 'active', condominioId: { [Op.ne]: null } } });
  if (!admin || !sindico) throw new Error('A validação requer administrador e condomínio já cadastrados. Nenhum usuário foi criado.');
  // ADMIN_GERAL may select an existing tenant for read-only inspection.
  const tenant = sindico.condominioId;
  const token = jwt.sign({ id: admin.id, perfil: 'ADMIN_GERAL', email: admin.email, tokenVersion: admin.tokenVersion || 0,
    condominioId: tenant }, process.env.JWT_SECRET, { expiresIn: '2m' });
  const headers = { Authorization: `Bearer ${token}` };
  for (const nome of nomes) {
    const resposta = await fetch(`http://${process.env.CONSUL_HOST || 'consul'}:8500/v1/health/service/${nome}?passing=true`);
    const instancias = await resposta.json();
    verificar(instancias.length === 1, `Consul ${nome}: ${instancias.length} instância saudável`);
  }
  const docs = ['/api/auth/docs/openapi.json', '/api/gestao/docs/openapi.json', '/api/financeiro/docs/openapi.json',
    '/api/meetings/v3/api-docs', '/api/plans/v3/api-docs', '/api/portaria/v3/api-docs', '/api/comunicacao/v3/api-docs'];
  for (const caminho of docs) {
    const resposta = await obter(caminho);
    verificar(resposta.status === 200 && (await resposta.json()).openapi, `OpenAPI ${caminho}: HTTP ${resposta.status}`);
  }
  const chamadas = ['/api/auth/me', '/api/gestao/receita', '/api/financeiro/contexto?condominioId=' + encodeURIComponent(tenant),
    '/api/meetings', '/api/plans', '/api/portaria/blocos', '/api/comunicacao/artigos', '/api/acessos/usuarios-condominio'];
  for (const caminho of chamadas) {
    const resposta = await obter(caminho, { headers });
    verificar(resposta.status === 200, `GET autenticado ${caminho.split('?')[0]}: HTTP ${resposta.status}`);
  }
  for (const caminho of ['/api/meetings', '/api/plans', '/api/portaria/blocos', '/api/comunicacao/artigos', '/api/acessos/guests']) {
    verificar((await obter(caminho)).status === 401, `Sem JWT continua bloqueado: ${caminho}`);
  }
  const origin = process.env.FRONTEND_URL || 'http://localhost:5173';
  for (const caminho of ['/api/auth/me', '/api/gestao/receita', '/api/financeiro/contexto', '/api/meetings', '/api/plans', '/api/portaria/blocos', '/api/comunicacao/artigos']) {
    const resposta = await obter(caminho, { method: 'OPTIONS', headers: { Origin: origin,
      'Access-Control-Request-Method': 'GET', 'Access-Control-Request-Headers': 'authorization,content-type' } });
    verificar(resposta.ok && [origin, '*'].includes(resposta.headers.get('access-control-allow-origin')), `CORS ${caminho}: HTTP ${resposta.status}`);
  }
  verificar((await obter('/api/meetings/swagger-ui.html')).status === 200, 'Swagger meeting acessível pelo gateway');
  verificar((await obter('/docs')).status === 200, 'Portal /docs acessível pelo gateway');
} catch (erro) {
  console.error('Validação interrompida: ' + erro.message);
  falhas++;
} finally {
  await sequelize.close();
  process.exitCode = falhas ? 1 : 0;
}
