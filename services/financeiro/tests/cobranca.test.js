// Cobrança recorrente e estorno: a tradução dos estados do Asaas, que é pura,
// e quem pode chamar cada rota nova — recusado antes de chegar ao banco ou ao
// gateway.
import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import jwt from 'jsonwebtoken';
import request from 'supertest';
import { statusDoPagamento, pagamentoDoAsaas, desfechoDoEstorno } from '../utils/asaasStatus.js';

process.env.JWT_SECRET = 'segredo-de-teste';
const { app } = await import('../app.js');

describe('estados do Asaas', () => {
  test('recebido, em atraso, estornado e o resto', () => {
    for (const s of ['RECEIVED', 'CONFIRMED', 'RECEIVED_IN_CASH']) assert.equal(statusDoPagamento(s), 'RECEBIDA');
    assert.equal(statusDoPagamento('OVERDUE'), 'VENCIDA');
    assert.equal(statusDoPagamento('REFUNDED'), 'ESTORNADA');
    assert.equal(statusDoPagamento('PENDING'), 'PENDENTE');
    assert.equal(statusDoPagamento('AWAITING_RISK_ANALYSIS'), 'PENDENTE');
  });

  test('estorno pedido ainda não devolveu: o pagamento segue recebido', () => {
    assert.equal(statusDoPagamento('REFUND_REQUESTED'), 'RECEBIDA');
    assert.equal(statusDoPagamento('REFUND_IN_PROGRESS'), 'RECEBIDA');
  });

  test('cobrança do Asaas vira linha de pagamento, em centavos', () => {
    const p = pagamentoDoAsaas({
      id: 'pay_1', value: 1000.1, dueDate: '2026-11-05', status: 'PENDING',
      billingType: 'BOLETO', invoiceUrl: 'https://sandbox.asaas.com/i/1', bankSlipUrl: 'https://sandbox.asaas.com/b/1',
    });
    assert.deepEqual(p, {
      asaasPagamentoId: 'pay_1', valorCentavos: 100010, vencimento: '2026-11-05', status: 'PENDENTE',
      forma: 'BOLETO', urlPagamento: 'https://sandbox.asaas.com/i/1', pagoEm: null,
    });
  });

  test('desfecho do estorno', () => {
    assert.equal(desfechoDoEstorno({ status: 'REFUNDED' }), 'CONCLUIDO');
    assert.equal(desfechoDoEstorno({ status: 'RECEIVED', refunds: [{ status: 'DONE' }] }), 'CONCLUIDO');
    // Aguardando autorização da ação no painel da conta: ainda não voltou.
    assert.equal(desfechoDoEstorno({ status: 'RECEIVED', refunds: [{ status: 'AWAITING_CRITICAL_ACTION_AUTHORIZATION' }] }), 'PENDENTE');
    assert.equal(desfechoDoEstorno({ status: 'RECEIVED', refunds: [{ status: 'DONE' }, { status: 'CANCELLED' }] }), 'FALHOU');
    assert.equal(desfechoDoEstorno({ status: 'RECEIVED' }), 'PENDENTE');
    assert.equal(desfechoDoEstorno(null), 'PENDENTE');
  });
});

const token = (perfil, extra = {}) => jwt.sign(
  { id: 7, perfil, tokenVersion: 0, email: 't@t', condominioId: 'cond-a', ...extra },
  process.env.JWT_SECRET,
);
const bearer = (perfil, extra) => ({ Authorization: `Bearer ${token(perfil, extra)}` });
const ID = '11111111-1111-1111-1111-111111111111';
const chamar = (metodo, caminho, cabecalhos = {}) =>
  request(app)[metodo](`/api/financeiro${caminho}`).set(cabecalhos).send({ motivo: 'teste de acesso' });

const ADMIN_GERAL = [
  ['post', '/plataforma/assinaturas'],
  ['get', '/plataforma/assinaturas'],
  ['patch', `/plataforma/assinaturas/${ID}/cancelar`],
  ['post', `/plataforma/pagamentos/${ID}/estornar`],
];

describe('acesso às rotas de cobrança', () => {
  for (const [metodo, caminho] of [...ADMIN_GERAL, ['get', '/plataforma/minha-assinatura'], ['post', `/admin/faturas/${ID}/estornar`], ['get', '/admin/estornos']]) {
    test(`${metodo.toUpperCase()} ${caminho} sem token → 401`, async () => {
      assert.equal((await chamar(metodo, caminho)).status, 401);
    });
  }

  for (const [metodo, caminho] of ADMIN_GERAL) {
    for (const perfil of ['ADMIN_SINDICO', 'MORADOR', 'PORTEIRO']) {
      test(`${metodo.toUpperCase()} ${caminho} como ${perfil} → 403`, async () => {
        assert.equal((await chamar(metodo, caminho, bearer(perfil))).status, 403);
      });
    }
  }

  for (const perfil of ['ADMIN_GERAL', 'MORADOR', 'DONO_ALUGUEL']) {
    test(`mensalidade do condomínio como ${perfil} → 403`, async () => {
      assert.equal((await chamar('get', '/plataforma/minha-assinatura', bearer(perfil))).status, 403);
    });
  }

  // Estornar é operar cobrança: o Admin Geral lê o financeiro do condomínio,
  // mas não devolve dinheiro de morador.
  for (const perfil of ['ADMIN_GERAL', 'MORADOR', 'DONO_ALUGUEL', 'PORTEIRO']) {
    test(`estornar fatura como ${perfil} → 403`, async () => {
      const extra = perfil === 'ADMIN_GERAL' ? { condominioId: undefined } : {};
      assert.equal((await chamar('post', `/admin/faturas/${ID}/estornar?condominioId=cond-a`, bearer(perfil, extra))).status, 403);
    });
  }

  test('estornos do condomínio como morador → 403', async () => {
    assert.equal((await chamar('get', '/admin/estornos', bearer('MORADOR'))).status, 403);
  });
});

describe('competência nas mensagens', async () => {
  const { rotulo } = await import('../utils/competencia.js');

  test('DATE do driver (meia-noite local) e texto dão o mesmo rótulo', () => {
    assert.equal(rotulo(new Date(2026, 8, 1)), 'set/2026');
    assert.equal(rotulo('2026-09-01'), 'set/2026');
    assert.equal(rotulo('2026-12'), 'dez/2026');
    assert.equal(rotulo(null), '');
  });
});
