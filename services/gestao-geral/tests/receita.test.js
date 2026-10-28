// Receita da plataforma: a conta vem pronta do plan-service (função
// receita_plataforma no banco). Aqui se confere só o que o gestao-geral
// acrescenta — nomes e quem não assina, do auth-api — com o fetch simulado.
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { montarReceita } from '../services/receitaService.js';

const RECEITA_DO_PLAN = {
  filtros: { meses: 3, plano: 'Completo' },
  planosDisponiveis: ['Básico', 'Completo'],
  indicadores: { mrrCentavos: 100000, clientesAtivos: 1, ticketMedioCentavos: 100000, acumuladoCentavos: 1000000 },
  porPlano: [{ plano: 'Completo', assinaturas: 1, mrrCentavos: 100000 }],
  evolucao: [{ mes: '2026-10', clientes: 1, mrrCentavos: 100000 }],
  clientes: [{ condominioId: 'cond-a', plano: 'Completo', status: 'ATIVA', vigente: true, desde: '2026-01-01', mensalidadeCentavos: 100000, acumuladoCentavos: 1000000 }],
  // cond-b assina outro plano: com o filtro, não aparece em `clientes`, mas
  // também não pode aparecer como "sem assinatura".
  condominiosComAssinatura: ['cond-a', 'cond-b'],
};

const CONDOMINIOS = [
  { id: 'cond-a', nome: 'Residencial A' },
  { id: 'cond-b', nome: 'Residencial B' },
  { id: 'cond-c', nome: 'Residencial C' },
];

const original = globalThis.fetch;
afterEach(() => { globalThis.fetch = original; });

/** Responde por trecho da URL; `null` simula a fonte fora do ar. */
function simular(respostas) {
  const pedidas = [];
  globalThis.fetch = async (url) => {
    pedidas.push(String(url));
    const [, corpo] = Object.entries(respostas).find(([trecho]) => String(url).includes(trecho));
    if (corpo === null) return new Response('{}', { status: 503 });
    return new Response(JSON.stringify(corpo), { status: 200 });
  };
  return pedidas;
}

test('usa a agregação do plan-service e acrescenta nomes e quem não assina', async () => {
  const pedidas = simular({
    '/api/assinaturas/receita': RECEITA_DO_PLAN,
    '/api/condominios': { condominios: CONDOMINIOS },
  });

  const r = await montarReceita('Bearer x', { meses: '3', plano: 'Completo' });

  assert.ok(pedidas.some((u) => u.includes('/api/assinaturas/receita?meses=3&plano=Completo')),
    'janela e plano viajam até o banco do plan-service');
  assert.ok(!pedidas.some((u) => /\/api\/assinaturas$/.test(u)), 'não baixa mais a lista de assinaturas');
  assert.equal(r.indicadores.mrrCentavos, 100000);
  assert.equal(r.indicadores.arrCentavos, 1200000);
  assert.deepEqual(r.semAssinatura, [{ condominioId: 'cond-c', nome: 'Residencial C' }]);
  assert.equal(r.indicadores.clientesSemAssinatura, 1);
  assert.equal(r.clientes[0].nome, 'Residencial A');
  assert.deepEqual(r.fontesIndisponiveis, []);
});

test('plan-service fora do ar é erro, e não receita zero', async () => {
  simular({ '/api/assinaturas/receita': null, '/api/condominios': { condominios: CONDOMINIOS } });
  const r = await montarReceita('Bearer x', {});
  assert.equal(r.erro, true);
  assert.equal(r.status, 503);
});

test('auth-api fora do ar: a receita aparece, sem nomes e sem a lista de quem não assina', async () => {
  simular({ '/api/assinaturas/receita': RECEITA_DO_PLAN, '/api/condominios': null });
  const r = await montarReceita('Bearer x', {});
  assert.equal(r.indicadores.mrrCentavos, 100000);
  assert.equal(r.clientes[0].nome, 'cond-a');
  assert.deepEqual(r.semAssinatura, []);
  assert.deepEqual(r.fontesIndisponiveis, ['auth-api']);
});
