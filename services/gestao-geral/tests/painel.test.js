import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  mesDe, ultimosMeses, destinatariosDoAviso, resumirOperacional, resumirEstrategico,
} from '../services/painelService.js';

// 15/10/2026, meio-dia em Brasília.
const AGORA = new Date('2026-10-15T15:00:00Z');

test('mês de data local e de data UTC', () => {
  assert.equal(mesDe('2026-10-14T14:00:00'), '2026-10', 'hora local sem fuso: o mês escrito');
  assert.equal(mesDe('2026-10-14'), '2026-10');
  // 01/11 às 01h UTC ainda é 31/10 em Brasília.
  assert.equal(mesDe('2026-11-01T01:00:00.000Z'), '2026-10');
  assert.equal(mesDe(null), null);
  assert.equal(mesDe('lixo'), null);
});

test('janela de meses atravessa a virada do ano', () => {
  assert.deepEqual(ultimosMeses(3, AGORA), ['2026-08', '2026-09', '2026-10']);
  assert.deepEqual(ultimosMeses(3, new Date('2027-01-10T15:00:00Z')), ['2026-11', '2026-12', '2027-01']);
});

test('destinatários do aviso seguem o público, e a gestão nunca entra', () => {
  const porPerfil = { MORADOR: 10, DONO_ALUGUEL: 2, PORTEIRO: 3, ADMIN_SINDICO: 1 };
  assert.equal(destinatariosDoAviso('TODOS', porPerfil), 15);
  assert.equal(destinatariosDoAviso('MORADORES', porPerfil), 12);
  assert.equal(destinatariosDoAviso('FUNCIONARIOS', porPerfil), 3);
  assert.equal(destinatariosDoAviso('SINDICO', porPerfil), 0);
});

test('operacional conta o que pede ação e aponta o mais antigo', () => {
  const r = resumirOperacional({
    entregas: [{ dataRecebimento: '2026-10-12T09:00' }, { dataRecebimento: '2026-10-10T18:00' }],
    reservasPendentes: [{ prazoDecisao: '2026-10-20T10:00:00' }, { prazoDecisao: '2026-10-16T08:00:00' }],
    pessoasDentro: [{}, {}, {}],
    visitantesDentro: [{}],
    // Já agregado pelo auth-api.
    ocorrencias: { abertas: 2, pendentes: 1, emAnalise: 1, maisAntigaDesde: '2026-09-20T12:00:00Z' },
    conversas: [
      { naoLidas: 2, encerradaEm: null },
      { naoLidas: 0, encerradaEm: null },
      { naoLidas: 5, encerradaEm: '2026-10-01' },
    ],
    // Panorama do comunicacao: só os vigentes, com as confirmações contadas.
    avisos: [
      { id: 'a', titulo: 'Moradores', dataFim: '2026-10-31', publicoAlvo: 'MORADORES', confirmacoes: 6 },
      { id: 'b', titulo: 'Todos', dataFim: '2026-10-20', publicoAlvo: 'TODOS', confirmacoes: 12 },
    ],
    porPerfil: { MORADOR: 10, DONO_ALUGUEL: 2 },
    kpis: { emAtraso: 3, abertas: 7, totalCentavos: 100000, pagasCentavos: 40000 },
  });

  assert.deepEqual(r.entregas, { aguardandoRetirada: 2, maisAntigaDesde: '2026-10-10T18:00' });
  assert.deepEqual(r.reservas, { aguardandoAprovacao: 2, proximoPrazo: '2026-10-16T08:00:00' });
  assert.deepEqual(r.presenca, { atendimentosAbertos: 3, visitantesDentro: 1 });
  assert.deepEqual(r.ocorrencias, { abertas: 2, pendentes: 1, emAnalise: 1, maisAntigaDesde: '2026-09-20T12:00:00Z' });
  assert.deepEqual(r.conversas, { abertas: 2, comMensagemNova: 1 }, 'encerrada não conta');
  assert.equal(r.avisos.vigentes, 2);
  assert.equal(r.avisos.leitura[0].id, 'a', 'a de menor leitura primeiro');
  assert.equal(r.avisos.leitura[0].percentual, 50, '6 de 12 moradores');
  assert.equal(r.avisos.leitura[1].percentual, 100);
  assert.equal(r.financeiro.faturasEmAtraso, 3);
});

test('fonte fora do ar vira bloco nulo, sem derrubar os outros', () => {
  const r = resumirOperacional({
    entregas: null, reservasPendentes: null, pessoasDentro: null, visitantesDentro: null,
    ocorrencias: { abertas: 0, pendentes: 0, emAnalise: 0, maisAntigaDesde: null },
    conversas: null, avisos: null, porPerfil: null, kpis: null,
  });

  assert.equal(r.entregas, null);
  assert.equal(r.reservas, null);
  assert.equal(r.presenca, null);
  assert.equal(r.conversas, null);
  assert.equal(r.avisos, null);
  assert.equal(r.financeiro, null);
  assert.equal(r.ocorrencias.abertas, 0, 'a fonte que respondeu continua no painel');

  assert.equal(resumirOperacional({ ocorrencias: null }).ocorrencias, null);
});

test('sem contagem de usuários não se inventa porcentagem de leitura', () => {
  const r = resumirOperacional({
    avisos: [{ id: 'a', titulo: 'x', dataFim: '2026-10-31', publicoAlvo: 'TODOS', confirmacoes: 4 }],
    porPerfil: null,
  });
  assert.equal(r.avisos.leitura[0].leituras, 4);
  assert.equal(r.avisos.leitura[0].percentual, null);
});

test('estratégico: séries mensais na janela do painel, com os agregados das fontes', () => {
  const meses = ['2026-08', '2026-09', '2026-10'];
  const r = resumirEstrategico({
    reservas: [
      { inicio: '2026-10-14T14:00:00', status: 'APROVADA', areaComumNome: 'Piscina' },
      { inicio: '2026-10-20T14:00:00', status: 'PENDENTE', areaComumNome: 'Piscina' },
      { inicio: '2026-09-02T10:00:00', status: 'CONCLUIDA', areaComumNome: 'Salão' },
      { inicio: '2026-10-21T10:00:00', status: 'CANCELADA', areaComumNome: 'Salão' },
      { inicio: '2026-05-01T10:00:00', status: 'APROVADA', areaComumNome: 'Salão' },
    ],
    // Resposta do auth-api: o mês de agosto não veio (sem ocorrência).
    ocorrencias: {
      porMes: [
        { mes: '2026-09', abertas: 1, resolvidas: 1 },
        { mes: '2026-10', abertas: 2, resolvidas: 1 },
      ],
      porCategoria: [{ categoria: 'Barulho', total: 2 }, { categoria: 'Limpeza', total: 1 }],
      tempoMedioResolucaoDias: 1.5,
    },
    kpisPorMes: {
      '2026-08': { total: 10, emAtraso: 1, pagasCentavos: 900 },
      '2026-09': null,
      '2026-10': { total: 0, emAtraso: 0, pagasCentavos: 0 },
    },
    // Resposta do financeiro: a soma chega como texto (bigint).
    multasPorMes: [{ mes: '2026-10', aplicadas: 2, canceladas: 1, valorCentavos: '15000' }],
  }, meses);

  assert.deepEqual(r.reservas.porMes.map((m) => m.total), [0, 1, 2]);
  assert.deepEqual(r.reservas.porArea, [{ area: 'Piscina', total: 2 }, { area: 'Salão', total: 1 }]);

  assert.deepEqual(r.ocorrencias.porMes.map((m) => [m.abertas, m.resolvidas]), [[0, 0], [1, 1], [2, 1]],
    'mês sem linha vira zero');
  assert.deepEqual(r.ocorrencias.porCategoria[0], { categoria: 'Barulho', total: 2 });
  assert.equal(r.ocorrencias.tempoMedioResolucaoDias, 1.5);

  assert.equal(r.inadimplencia.porMes[0].taxa, 10);
  assert.equal(r.inadimplencia.porMes[1].taxa, null, 'mês sem resposta fica nulo');
  assert.equal(r.inadimplencia.porMes[2].taxa, null, 'sem fatura não há taxa');

  assert.deepEqual(r.multas.porMes[0], { mes: '2026-08', aplicadas: 0, canceladas: 0, valorCentavos: 0 });
  assert.deepEqual(r.multas.porMes[2], { mes: '2026-10', aplicadas: 2, canceladas: 1, valorCentavos: 15000 });
});

test('estratégico: fonte agregada fora do ar vira bloco nulo', () => {
  const r = resumirEstrategico({ reservas: [], ocorrencias: null, kpisPorMes: null, multasPorMes: null }, ['2026-10']);
  assert.equal(r.ocorrencias, null);
  assert.equal(r.multas, null);
  assert.equal(r.inadimplencia, null);
  assert.deepEqual(r.reservas.porMes, [{ mes: '2026-10', total: 0 }]);
});
