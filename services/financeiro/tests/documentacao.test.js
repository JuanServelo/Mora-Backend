// A especificação OpenAPI cobre toda rota do Express, com sumário, descrição
// e exemplos. Rota nova sem documentação quebra aqui.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import request from 'supertest';

process.env.JWT_SECRET ??= 'segredo-de-teste';
const { app } = await import('../app.js');
const { rotasDe } = await import('./helpers/rotas.js');
const { problemasDeDocumentacao } = await import('./helpers/documentacao.js');
const { openapi } = await import('../docs/openapi.js');

test('toda rota está documentada, com sumário, descrição e exemplos', () => {
  assert.deepEqual(problemasDeDocumentacao(rotasDe(app), openapi), []);
});

test('a especificação é servida sem token', async () => {
  const r = await request(app).get('/api/financeiro/docs/openapi.json');
  assert.equal(r.status, 200);
  assert.equal(r.body.info.title, 'financeiro');
});
