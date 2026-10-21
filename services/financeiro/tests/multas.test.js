import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  dataLocal, somarDias, prazoRecurso, dentroDoPrazo, podeRecorrer, faturavel,
  estornoPendente, transicao,
} from '../utils/multas.js';
import { agruparMultas } from '../services/faturamentoService.js';

// 10/09/2026 às 12h em Brasília.
const APLICADA = new Date('2026-09-10T15:00:00Z');
const dia = (iso) => new Date(`${iso}T15:00:00Z`);

const multa = (extra = {}) => ({
  id: 'm1', unidadeId: 'u1', status: 'APLICADA', valorCentavos: 15000, motivo: 'Barulho',
  criadoEm: APLICADA, recursoTexto: null, faturaItemId: null, estornoFaturaItemId: null,
  ...extra,
});

test('a data local não muda de dia por causa do fuso', () => {
  // 23h30 em Brasília já é dia seguinte em UTC.
  assert.equal(dataLocal(new Date('2026-09-11T02:30:00Z')), '2026-09-10');
});

test('somarDias atravessa o fim do mês e do ano', () => {
  assert.equal(somarDias('2026-01-25', 15), '2026-02-09');
  assert.equal(somarDias('2026-12-25', 10), '2027-01-04');
});

test('o prazo de recurso conta da aplicação, e o último dia vale', () => {
  assert.equal(prazoRecurso(APLICADA, 15), '2026-09-25');
  assert.equal(dentroDoPrazo(APLICADA, 15, dia('2026-09-25')), true);
  assert.equal(dentroDoPrazo(APLICADA, 15, dia('2026-09-26')), false);
});

test('prazo zero: só dá para recorrer no próprio dia', () => {
  assert.equal(dentroDoPrazo(APLICADA, 0, dia('2026-09-10')), true);
  assert.equal(dentroDoPrazo(APLICADA, 0, dia('2026-09-11')), false);
});

test('recurso: um só, e só em multa aplicada e ainda não cobrada', () => {
  const hoje = dia('2026-09-12');
  assert.equal(podeRecorrer(multa(), 15, hoje), true);
  assert.equal(podeRecorrer(multa({ recursoTexto: 'já recorri' }), 15, hoje), false);
  assert.equal(podeRecorrer(multa({ status: 'EM_RECURSO' }), 15, hoje), false);
  assert.equal(podeRecorrer(multa({ faturaItemId: 'i1' }), 15, hoje), false);
  assert.equal(podeRecorrer(multa(), 15, dia('2026-09-30')), false);
});

test('não se cobra multa dentro do prazo de recurso nem em recurso', () => {
  assert.equal(faturavel(multa(), 15, dia('2026-09-20')), false, 'ainda no prazo');
  assert.equal(faturavel(multa(), 15, dia('2026-09-26')), true, 'prazo vencido');
  assert.equal(faturavel(multa({ status: 'EM_RECURSO' }), 15, dia('2026-10-30')), false);
  assert.equal(faturavel(multa({ faturaItemId: 'i1' }), 15, dia('2026-10-30')), false, 'já cobrada');
});

test('estorno pendente: cancelada depois de cobrada, sem estorno lançado', () => {
  assert.equal(estornoPendente(multa({ status: 'CANCELADA', faturaItemId: 'i1' })), true);
  assert.equal(estornoPendente(multa({ status: 'CANCELADA' })), false, 'nunca cobrada');
  assert.equal(
    estornoPendente(multa({ status: 'CANCELADA', faturaItemId: 'i1', estornoFaturaItemId: 'i2' })),
    false, 'já estornada',
  );
});

test('transições: CANCELADA é final', () => {
  assert.equal(transicao('recorrer', 'APLICADA'), 'EM_RECURSO');
  assert.equal(transicao('aceitarRecurso', 'EM_RECURSO'), 'CANCELADA');
  assert.equal(transicao('recusarRecurso', 'EM_RECURSO'), 'APLICADA');
  assert.equal(transicao('cancelar', 'EM_RECURSO'), 'CANCELADA');
  assert.equal(transicao('cancelar', 'CANCELADA'), null);
  assert.equal(transicao('recorrer', 'EM_RECURSO'), null);
  assert.equal(transicao('julgar', 'APLICADA'), null, 'ação desconhecida');
});

test('fechamento separa por unidade o que cobrar e o que estornar', () => {
  const hoje = dia('2026-10-05');
  const grupos = agruparMultas([
    multa({ id: 'cobrar' }),
    multa({ id: 'no-prazo', criadoEm: dia('2026-10-01') }),
    multa({ id: 'em-recurso', status: 'EM_RECURSO' }),
    multa({ id: 'estornar', status: 'CANCELADA', faturaItemId: 'i9' }),
    multa({ id: 'outra-unidade', unidadeId: 'u2' }),
  ], 15, hoje);

  assert.deepEqual(grupos.get('u1').cobrar.map((m) => m.id), ['cobrar']);
  assert.deepEqual(grupos.get('u1').estornar.map((m) => m.id), ['estornar']);
  assert.deepEqual(grupos.get('u2').cobrar.map((m) => m.id), ['outra-unidade']);
});

test('unidade só com multa no prazo não entra no agrupamento', () => {
  const grupos = agruparMultas([multa({ criadoEm: dia('2026-10-01') })], 15, dia('2026-10-05'));
  assert.equal(grupos.size, 0);
});
