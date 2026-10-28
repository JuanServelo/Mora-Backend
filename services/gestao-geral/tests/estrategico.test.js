// Painel estratégico: a inadimplência mês a mês vem numa chamada só, agrupada
// no banco do financeiro (função kpis_faturas_por_mes), e não uma por mês.
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { montarEstrategico, ultimosMeses } from '../services/painelService.js';

const original = globalThis.fetch;
afterEach(() => { globalThis.fetch = original; });

function simular(financeiroFora = false) {
  const pedidas = [];
  globalThis.fetch = async (url) => {
    const u = String(url);
    pedidas.push(u);
    if (u.includes('/admin/faturas/kpis/por-mes')) {
      if (financeiroFora) return new Response('{}', { status: 503 });
      const meses = ultimosMeses(3).map((mes, i) => ({
        mes, total: 4, totalCentavos: 400000, pagas: 4 - i, pagasCentavos: (4 - i) * 100000, abertas: i, emAtraso: i,
      }));
      return new Response(JSON.stringify({ sucesso: true, meses }), { status: 200 });
    }
    if (u.includes('/admin/multas/por-mes')) return new Response(JSON.stringify({ sucesso: true, meses: [] }), { status: 200 });
    if (u.includes('/reclamacoes/estatisticas')) return new Response(JSON.stringify({ porMes: [], porCategoria: [] }), { status: 200 });
    return new Response('[]', { status: 200 });
  };
  return pedidas;
}

test('inadimplência: uma chamada para a janela inteira', async () => {
  const pedidas = simular();
  const r = await montarEstrategico('cond-a', 3, 'Bearer x');

  const kpis = pedidas.filter((u) => u.includes('/admin/faturas/kpis'));
  assert.equal(kpis.length, 1, 'antes eram três, uma por mês');
  assert.ok(kpis[0].includes('/admin/faturas/kpis/por-mes?meses=3'));

  assert.deepEqual(r.inadimplencia.porMes.map((m) => m.taxa), [0, 25, 50]);
  assert.equal(r.inadimplencia.porMes[2].recebidoCentavos, 200000);
  assert.deepEqual(r.fontesIndisponiveis, []);
});

test('financeiro fora do ar: bloco nulo e fonte apontada', async () => {
  simular(true);
  const r = await montarEstrategico('cond-a', 3, 'Bearer x');
  assert.equal(r.inadimplencia, null);
  assert.deepEqual(r.fontesIndisponiveis, ['financeiro']);
});
