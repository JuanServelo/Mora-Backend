// Quem pode chamar cada rota nova de multas, contratos e prestação de contas.
//
// Nenhum destes casos chega ao banco: autenticação, perfil e escopo recusam
// antes. É o que permite rodar sem Postgres — e é também o que se quer provar,
// que a recusa acontece cedo.
import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import jwt from 'jsonwebtoken';
import request from 'supertest';

process.env.JWT_SECRET = 'segredo-de-teste';
const { app } = await import('../app.js');

const token = (perfil, extra = {}) => jwt.sign(
  { id: 7, perfil, tokenVersion: 0, email: 't@t', condominioId: 'cond-a', ...extra },
  process.env.JWT_SECRET,
);
const bearer = (perfil, extra) => ({ Authorization: `Bearer ${token(perfil, extra)}` });

const BASE = '/api/financeiro';
const ID = '11111111-1111-1111-1111-111111111111';

const ROTAS = [
  ['get', '/admin/multas'],
  ['post', '/admin/multas'],
  ['patch', `/admin/multas/${ID}/julgar`],
  ['patch', `/admin/multas/${ID}/cancelar`],
  ['get', '/multas/minhas'],
  ['post', `/multas/${ID}/recurso`],
  ['get', '/admin/contratos'],
  ['post', '/admin/contratos'],
  ['patch', `/admin/contratos/${ID}/encerrar`],
  ['get', '/contratos/meus'],
  ['post', '/contratos/meus'],
  ['patch', `/contratos/${ID}/encerrar`],
  ['get', '/admin/prestacao/2026-09'],
  ['post', '/admin/prestacao/2026-09/lancamentos'],
  ['delete', `/admin/prestacao/lancamentos/${ID}`],
  ['post', '/admin/prestacao/2026-09/publicar'],
  ['get', '/prestacao'],
  ['get', '/prestacao/2026-09'],
];

const chamar = (metodo, caminho, cabecalhos = {}) =>
  request(app)[metodo](BASE + caminho).set(cabecalhos).send({});

describe('sem token', () => {
  for (const [metodo, caminho] of ROTAS) {
    test(`${metodo.toUpperCase()} ${caminho} → 401`, async () => {
      const r = await chamar(metodo, caminho);
      assert.equal(r.status, 401);
    });
  }

  test('token assinado com outro segredo → 401', async () => {
    const falso = jwt.sign({ id: 1, perfil: 'ADMIN_SINDICO', condominioId: 'cond-a' }, 'outro');
    const r = await chamar('get', '/admin/multas', { Authorization: `Bearer ${falso}` });
    assert.equal(r.status, 401);
  });
});

describe('perfil errado → 403', () => {
  // [método, rota, perfil que não pode]
  const CASOS = [
    ['get', '/admin/multas', 'MORADOR'],
    ['get', '/admin/multas', 'PORTEIRO'],
    ['post', '/admin/multas', 'MORADOR'],
    // O Admin Geral acompanha, mas não aplica nem julga multa.
    ['post', '/admin/multas', 'ADMIN_GERAL'],
    ['patch', `/admin/multas/${ID}/julgar`, 'ADMIN_GERAL'],
    ['patch', `/admin/multas/${ID}/cancelar`, 'MORADOR'],
    // Recurso é do morador; o síndico julga, não recorre.
    ['post', `/multas/${ID}/recurso`, 'ADMIN_SINDICO'],
    ['get', '/multas/minhas', 'PORTEIRO'],
    ['get', '/admin/contratos', 'DONO_ALUGUEL'],
    ['post', '/admin/contratos', 'ADMIN_GERAL'],
    // Contrato é cadastrado pelo dono; o inquilino só consulta.
    ['post', '/contratos/meus', 'MORADOR'],
    ['patch', `/contratos/${ID}/encerrar`, 'MORADOR'],
    ['get', '/admin/prestacao/2026-09', 'MORADOR'],
    ['post', '/admin/prestacao/2026-09/lancamentos', 'ADMIN_GERAL'],
    ['post', '/admin/prestacao/2026-09/publicar', 'DONO_ALUGUEL'],
    ['delete', `/admin/prestacao/lancamentos/${ID}`, 'PORTEIRO'],
    ['get', '/prestacao', 'ADMIN_SINDICO'],
    ['get', '/prestacao/2026-09', 'PORTEIRO'],
  ];

  for (const [metodo, caminho, perfil] of CASOS) {
    test(`${perfil} em ${metodo.toUpperCase()} ${caminho}`, async () => {
      const r = await chamar(metodo, caminho, bearer(perfil));
      assert.equal(r.status, 403);
      assert.equal(r.body.sucesso, false);
    });
  }
});

describe('escopo', () => {
  test('morador sem condomínio no token → 400, sem chegar ao banco', async () => {
    const r = await chamar('get', '/prestacao', bearer('MORADOR', { condominioId: null }));
    assert.equal(r.status, 400);
  });

  test('síndico sem condomínio no token → 400', async () => {
    const r = await chamar('get', '/admin/multas', bearer('ADMIN_SINDICO', { condominioId: null }));
    assert.equal(r.status, 400);
  });
});

describe('rotas removidas', () => {
  test('as rotas antigas de notificação não existem mais', async () => {
    const r = await chamar('get', '/notificacoes', bearer('MORADOR'));
    assert.equal(r.status, 404);
  });
});
