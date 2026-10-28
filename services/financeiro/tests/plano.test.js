import { test } from 'node:test';
import assert from 'node:assert/strict';
import { exigirPlano } from '../middleware/plano.js';
import request from 'supertest';
import jwt from 'jsonwebtoken';

test('plano: libera somente 204, repassa token e bloqueia indisponibilidade', async (t) => {
  let upstream = 204;
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    assert.ok(url.endsWith('/api/plans/access/financeiro'));
    assert.equal(options.headers.Authorization, 'Bearer test-token');
    if (upstream === 'offline') throw new Error('offline');
    return { status: upstream };
  });
  for (const [reply, expected] of [[204, null], [401, 401], [403, 403], [500, 503], ['offline', 503]]) {
    upstream = reply;
    let called = false;
    const res = { status(code) { this.code = code; return this; }, json() { return this; } };
    await exigirPlano({ headers: { authorization: 'Bearer test-token' } }, res, () => { called = true; });
    assert.equal(called, expected === null);
    assert.equal(res.code ?? null, expected);
  }
});

test('plano: sem token recusa antes de consultar serviço', async (t) => {
  t.mock.method(globalThis, 'fetch', () => { assert.fail('não deveria consultar'); });
  const res = { status(code) { this.code = code; return this; }, json() {} };
  await exigirPlano({ headers: {} }, res, () => assert.fail('não deveria liberar'));
  assert.equal(res.code, 401);
});

test('API direta bloqueia módulo antes de consultar o banco e mantém docs públicas', async (t) => {
  process.env.JWT_SECRET = 'segredo-de-teste';
  const { app } = await import('../app.js');
  const token = jwt.sign({ id: 1, perfil: 'ADMIN_SINDICO', condominioId: 'qa-sem-plano' }, process.env.JWT_SECRET);
  t.mock.method(globalThis, 'fetch', async () => ({ status: 403 }));
  const denied = await request(app).get('/api/financeiro/config').set('Authorization', `Bearer ${token}`);
  assert.equal(denied.status, 403);
  const docs = await request(app).get('/api/financeiro/docs/openapi.json');
  assert.equal(docs.status, 200);
});
