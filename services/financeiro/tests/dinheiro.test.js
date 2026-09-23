import { test } from 'node:test';
import assert from 'node:assert/strict';
import { paraCentavos, paraDecimalString, ratear } from '../utils/dinheiro.js';

test('paraCentavos aceita os formatos que o usuário digita', () => {
  assert.equal(paraCentavos('249,90'), 24990);
  assert.equal(paraCentavos('249.90'), 24990);
  assert.equal(paraCentavos('1.234,56'), 123456);
  assert.equal(paraCentavos(249.9), 24990);
  assert.equal(paraCentavos('350'), 35000);
});

test('paraCentavos recusa o que não é valor', () => {
  assert.equal(paraCentavos('abc'), null);
  assert.equal(paraCentavos('12,345'), null, 'três casas decimais não é dinheiro');
  assert.equal(paraCentavos(''), null);
});

test('paraDecimalString devolve o formato que o gateway espera', () => {
  assert.equal(paraDecimalString(24990), '249.90');
  assert.equal(paraDecimalString(5), '0.05');
  assert.equal(paraDecimalString(100), '1.00');
});

test('rateio proporcional fecha exatamente com o total', () => {
  // O caso que a divisão ingênua erra: R$ 1.000,00 entre três frações iguais
  // daria 333,33 três vezes = R$ 999,99, e um centavo some todo mês.
  const pesos = [
    { chave: 'a', peso: 1 },
    { chave: 'b', peso: 1 },
    { chave: 'c', peso: 1 },
  ];
  const parcelas = ratear(100000, pesos);
  const soma = [...parcelas.values()].reduce((a, b) => a + b, 0);

  assert.equal(soma, 100000, 'a soma das parcelas precisa bater com o total');
  assert.equal([...parcelas.values()].filter((v) => v === 33334).length, 1);
  assert.equal([...parcelas.values()].filter((v) => v === 33333).length, 2);
});

test('rateio por fração ideal desigual fecha com o total', () => {
  const pesos = [
    { chave: 'cobertura', peso: 180 },
    { chave: 'ap-101', peso: 137 },
    { chave: 'ap-102', peso: 137 },
    { chave: 'ap-103', peso: 137 },
    { chave: 'ap-104', peso: 137 },
    { chave: 'studio', peso: 272 },
  ];
  const total = 4735711; // valor propositalmente feio
  const parcelas = ratear(total, pesos);
  const soma = [...parcelas.values()].reduce((a, b) => a + b, 0);

  assert.equal(soma, total);
  // A sobra vai para a maior fração, e não se espalha.
  assert.ok(parcelas.get('studio') >= parcelas.get('cobertura'));
});

test('rateio recusa soma de pesos zerada em vez de dividir por zero', () => {
  assert.throws(() => ratear(1000, [{ chave: 'a', peso: 0 }]), /pesos/);
});
