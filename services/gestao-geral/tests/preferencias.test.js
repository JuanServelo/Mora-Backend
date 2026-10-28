// Preferências dos painéis: quem passa pelas barreiras antes de chegar ao
// banco. O auth-api é simulado trocando o `fetch` global — nenhum caso aqui
// chega a consultar o Postgres.
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import jwt from 'jsonwebtoken';
import request from 'supertest';

process.env.JWT_SECRET ??= 'segredo-de-teste';
const { app } = await import('../app.js');
const { validarPreferencia, TAMANHO_MAXIMO } = await import('../routes/preferencias.js');

const fetchOriginal = globalThis.fetch;
afterEach(() => { globalThis.fetch = fetchOriginal; });

/** O auth-api responde `/api/auth/me` com este status e corpo. */
function authApiResponde(status, corpo = {}) {
  const chamadas = [];
  globalThis.fetch = async (url, opcoes) => {
    chamadas.push({ url: String(url), authorization: opcoes?.headers?.Authorization });
    return new Response(JSON.stringify(corpo), { status, headers: { 'Content-Type': 'application/json' } });
  };
  return chamadas;
}

const token = jwt.sign({ id: 26, perfil: 'ADMIN_SINDICO', condominioId: 'cond-a', tokenVersion: 0 }, process.env.JWT_SECRET);
const comToken = (r) => r.set('Authorization', `Bearer ${token}`);
const URL_OK = '/api/gestao/preferencias/painel.condominio.operacional';

test('validarPreferencia: aceita as chaves dos painéis e um objeto', () => {
  const valor = { versao: 2, blocos: [{ id: 'ocorrencias', visivel: true, cor: 'coral' }] };
  assert.equal(validarPreferencia('painel.geral', valor), null);
  assert.equal(validarPreferencia('painel.condominio.estrategico', valor), null);
});

test('validarPreferencia: recusa chave fora do padrão, não-objeto e valor grande', () => {
  assert.match(validarPreferencia('qualquer', {}), /desconhecida/);
  assert.match(validarPreferencia('painel.', {}), /desconhecida/);
  assert.match(validarPreferencia("painel.geral'; drop", {}), /desconhecida/);
  assert.match(validarPreferencia('painel.geral', null), /objeto/);
  assert.match(validarPreferencia('painel.geral', [1]), /objeto/);
  assert.match(validarPreferencia('painel.geral', { x: 'y'.repeat(TAMANHO_MAXIMO) }), /grande/);
});

test('sem token → 401, sem perguntar ao auth-api', async () => {
  const chamadas = authApiResponde(200, { usuario: { id: 26 } });
  const r = await request(app).get(URL_OK);
  assert.equal(r.status, 401);
  assert.equal(chamadas.length, 0);
});

test('token de outro segredo → 401', async () => {
  const falso = jwt.sign({ id: 26, perfil: 'ADMIN_SINDICO' }, 'outro-segredo');
  const r = await request(app).get(URL_OK).set('Authorization', `Bearer ${falso}`);
  assert.equal(r.status, 401);
});

test('chave desconhecida → 400, antes de ir ao auth-api', async () => {
  const chamadas = authApiResponde(200, { usuario: { id: 26 } });
  const r = await comToken(request(app).get('/api/gestao/preferencias/outra-coisa'));
  assert.equal(r.status, 400);
  assert.equal(chamadas.length, 0);
});

test('sessão encerrada no auth-api (token de assinatura válida) → 401', async () => {
  const chamadas = authApiResponde(401, { sucesso: false, mensagem: 'Sessão encerrada' });
  const r = await comToken(request(app).get(URL_OK));
  assert.equal(r.status, 401);
  assert.equal(chamadas.length, 1);
  assert.match(chamadas[0].url, /\/api\/auth\/me$/);
  assert.equal(chamadas[0].authorization, `Bearer ${token}`, 'repassa o token de quem pediu');
});

test('auth-api fora do ar → 503, falha fechada', async () => {
  globalThis.fetch = async () => { throw new Error('ECONNREFUSED'); };
  const r = await comToken(request(app).put(URL_OK).send({ valor: {} }));
  assert.equal(r.status, 503);
});
