import { test } from 'node:test';
import assert from 'node:assert/strict';
import { resumir, urlSegura } from '../utils/prestacao.js';

const l = (tipo, categoria, valorCentavos) => ({ tipo, categoria, valorCentavos });

test('totais somam a receita das taxas aos lançamentos', () => {
  const r = resumir([
    l('DESPESA', 'Limpeza', 120000),
    l('DESPESA', 'Água', 80000),
    l('RECEITA', 'Aluguel do salão', 30000),
  ], 500000);

  assert.equal(r.receitasCentavos, 530000);
  assert.equal(r.despesasCentavos, 200000);
  assert.equal(r.saldoCentavos, 330000);
});

test('categorias são agrupadas e ordenadas do maior para o menor', () => {
  const r = resumir([
    l('DESPESA', 'Limpeza', 50000),
    l('DESPESA', 'Água', 80000),
    l('DESPESA', 'Limpeza', 40000),
  ], 0);

  assert.deepEqual(r.porCategoria.map((c) => [c.categoria, c.valorCentavos]), [
    ['Limpeza', 90000],
    ['Água', 80000],
  ]);
});

test('a receita automática aparece marcada, e só quando existe', () => {
  const com = resumir([], 1000);
  assert.equal(com.porCategoria[0].automatica, true);
  assert.equal(com.porCategoria[0].categoria, 'Taxas condominiais');

  assert.equal(resumir([], 0).porCategoria.length, 0);
});

test('mesma categoria em receita e despesa não se misturam', () => {
  const r = resumir([l('RECEITA', 'Outros', 100), l('DESPESA', 'Outros', 300)], 0);
  assert.equal(r.porCategoria.length, 2);
  assert.equal(r.saldoCentavos, -200);
});

test('comprovante: só http(s); vazio é permitido', () => {
  assert.equal(urlSegura('https://drive.example.com/nota.pdf'), 'https://drive.example.com/nota.pdf');
  assert.equal(urlSegura(''), null);
  assert.equal(urlSegura(null), null);
  assert.equal(urlSegura('javascript:alert(1)'), undefined);
  assert.equal(urlSegura('não é link'), undefined);
});
