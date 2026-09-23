import app from '../../app.js';
import { chavesDe } from '../helpers/rotas.js';
import { INVENTARIO } from './inventario.js';

/**
 * A trava que mantém a cobertura honesta.
 *
 * Sem ela, "todas as rotas são testadas" vale no dia em que foi escrito e
 * decai em silêncio: cada endpoint novo entra sem teste, a suíte segue verde, e
 * a afirmação vira folclore. Comparar o inventário com o que o Express
 * registrou de fato transforma esse decaimento num teste vermelho.
 *
 * A comparação é nos dois sentidos de propósito. Rota sem entrada é endpoint
 * sem contrato declarado; entrada sem rota é teste que não exercita mais nada e
 * dá uma sensação de cobertura que não existe.
 */
describe('inventário de rotas', () => {
  const registradas = chavesDe(app);
  const inventariadas = Object.keys(INVENTARIO).sort();

  test('toda rota registrada tem contrato declarado', () => {
    const semContrato = registradas.filter((r) => !(r in INVENTARIO));
    expect(semContrato).toEqual([]);
  });

  test('todo contrato declarado corresponde a uma rota real', () => {
    const orfaos = inventariadas.filter((r) => !registradas.includes(r));
    expect(orfaos).toEqual([]);
  });

  test('não há rota duplicada', () => {
    expect(new Set(registradas).size).toBe(registradas.length);
  });
});
