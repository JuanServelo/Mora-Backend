// Cotação do dólar: o Banco Central é simulado trocando o `fetch` global —
// nenhum caso aqui sai para a internet.
import { test, afterEach, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import jwt from 'jsonwebtoken';
import request from 'supertest';

process.env.JWT_SECRET ??= 'segredo-de-teste';
const { app } = await import('../app.js');
const { obterCotacaoDolar, limparCache, VALIDADE_MS } = await import('../services/cambioService.js');

const fetchOriginal = globalThis.fetch;
beforeEach(() => limparCache());
afterEach(() => { globalThis.fetch = fetchOriginal; });

/** A PTAX responde com este status e esta lista de cotações. */
function ptaxResponde(status, value = []) {
  const chamadas = [];
  globalThis.fetch = async (url) => {
    chamadas.push(String(url));
    return new Response(JSON.stringify({ value }), { status, headers: { 'Content-Type': 'application/json' } });
  };
  return chamadas;
}

const COTACAO = { cotacaoCompra: 5.334, cotacaoVenda: 5.3346, dataHoraCotacao: '2026-10-27 13:05:26.521898' };

const token = jwt.sign({ id: 41, perfil: 'MORADOR', condominioId: 'cond-a', tokenVersion: 0 }, process.env.JWT_SECRET);
const comToken = (r) => r.set('Authorization', `Bearer ${token}`);

test('sem token → 401, sem consultar o Banco Central', async () => {
  const chamadas = ptaxResponde(200, [COTACAO]);
  const r = await request(app).get('/api/gestao/cambio');
  assert.equal(r.status, 401);
  assert.equal(chamadas.length, 0);
});

test('qualquer perfil logado recebe a cotação de venda', async () => {
  ptaxResponde(200, [COTACAO]);
  const r = await comToken(request(app).get('/api/gestao/cambio'));
  assert.equal(r.status, 200);
  assert.deepEqual(r.body, {
    sucesso: true, moeda: 'USD', reaisPorDolar: 5.3346, dataCotacao: '2026-10-27', fonte: 'Banco Central (PTAX)',
  });
});

test('pede a mais recente dos últimos dias, com as datas no formato da PTAX', async () => {
  const chamadas = ptaxResponde(200, [COTACAO]);
  await obterCotacaoDolar(Date.parse('2026-10-28T12:00:00Z'));
  assert.doesNotMatch(chamadas[0], /\+/, 'a PTAX recusa espaço codificado como +');
  const url = decodeURIComponent(chamadas[0]);
  assert.match(url, /@dataInicial='10-21-2026'/);
  assert.match(url, /@dataFinalCotacao='10-28-2026'/);
  assert.match(url, /\$orderby=dataHoraCotacao desc/);
  assert.match(url, /\$top=1/);
});

test('dentro da validade, não consulta de novo', async () => {
  const chamadas = ptaxResponde(200, [COTACAO]);
  const agora = Date.now();
  await obterCotacaoDolar(agora);
  await obterCotacaoDolar(agora + VALIDADE_MS - 1);
  assert.equal(chamadas.length, 1);
});

test('vencida e o Banco Central fora do ar → devolve a última conhecida', async () => {
  ptaxResponde(200, [COTACAO]);
  const agora = Date.now();
  await obterCotacaoDolar(agora);

  const chamadas = ptaxResponde(500);
  const cotacao = await obterCotacaoDolar(agora + VALIDADE_MS + 1);
  assert.equal(chamadas.length, 1);
  assert.deepEqual(cotacao, { reaisPorDolar: 5.3346, dataCotacao: '2026-10-27' });
});

test('Banco Central fora do ar e nada guardado → 503', async () => {
  ptaxResponde(500);
  const r = await comToken(request(app).get('/api/gestao/cambio'));
  assert.equal(r.status, 503);
  assert.equal(r.body.sucesso, false);
});

test('período sem cotação ou valor inválido → 503, sem guardar', async () => {
  ptaxResponde(200, []);
  assert.equal((await obterCotacaoDolar()).status, 503);

  const chamadas = ptaxResponde(200, [{ ...COTACAO, cotacaoVenda: 0 }]);
  assert.equal((await obterCotacaoDolar()).status, 503);
  await obterCotacaoDolar();
  assert.equal(chamadas.length, 2);
});
