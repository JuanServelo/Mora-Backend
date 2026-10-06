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
    reclamacoes: [
      { status: 'PENDENTE', createdAt: '2026-10-01T12:00:00Z' },
      { status: 'EM_ANALISE', createdAt: '2026-09-20T12:00:00Z' },
      { status: 'RESOLVIDO', createdAt: '2026-08-01T12:00:00Z' },
    ],
    conversas: [
      { naoLidas: 2, encerradaEm: null },
      { naoLidas: 0, encerradaEm: null },
      { naoLidas: 5, encerradaEm: '2026-10-01' },
    ],
    avisos: [
      { id: 'a', titulo: 'Vigente', publicado: true, dataInicio: '2026-10-01', dataFim: '2026-10-31', publicoAlvo: 'MORADORES' },
      { id: 'b', titulo: 'Vencido', publicado: true, dataInicio: '2026-09-01', dataFim: '2026-09-30', publicoAlvo: 'TODOS' },
      { id: 'c', titulo: 'Rascunho', publicado: false, dataInicio: '2026-10-01', dataFim: '2026-10-31', publicoAlvo: 'TODOS' },
    ],
    leituras: { a: 6 },
    porPerfil: { MORADOR: 10, DONO_ALUGUEL: 2 },
    kpis: { emAtraso: 3, abertas: 7, totalCentavos: 100000, pagasCentavos: 40000 },
  }, AGORA);

  assert.deepEqual(r.entregas, { aguardandoRetirada: 2, maisAntigaDesde: '2026-10-10T18:00' });
  assert.deepEqual(r.reservas, { aguardandoAprovacao: 2, proximoPrazo: '2026-10-16T08:00:00' });
  assert.deepEqual(r.presenca, { atendimentosAbertos: 3, visitantesDentro: 1 });
  assert.equal(r.ocorrencias.abertas, 2, 'resolvida não conta como aberta');
  assert.equal(r.ocorrencias.maisAntigaDesde, '2026-09-20T12:00:00Z');
  assert.deepEqual(r.conversas, { abertas: 2, comMensagemNova: 1 }, 'encerrada não conta');
  assert.equal(r.avisos.vigentes, 1, 'nem vencido nem rascunho');
  assert.equal(r.avisos.leitura[0].percentual, 50, '6 de 12 moradores');
  assert.equal(r.financeiro.faturasEmAtraso, 3);
});

test('fonte fora do ar vira bloco nulo, sem derrubar os outros', () => {
  const r = resumirOperacional({
    entregas: null, reservasPendentes: null, pessoasDentro: null, visitantesDentro: null,
    reclamacoes: [], conversas: null, avisos: null, leituras: null, porPerfil: null, kpis: null,
  }, AGORA);

  assert.equal(r.entregas, null);
  assert.equal(r.reservas, null);
  assert.equal(r.presenca, null);
  assert.equal(r.avisos, null);
  assert.equal(r.financeiro, null);
  assert.deepEqual(r.ocorrencias.abertas, 0, 'a fonte que respondeu continua no painel');
});

test('sem contagem de usuários não se inventa porcentagem de leitura', () => {
  const r = resumirOperacional({
    avisos: [{ id: 'a', titulo: 'x', publicado: true, dataInicio: '2026-10-01', dataFim: '2026-10-31', publicoAlvo: 'TODOS' }],
    leituras: { a: 4 }, porPerfil: null,
  }, AGORA);
  assert.equal(r.avisos.leitura[0].leituras, 4);
  assert.equal(r.avisos.leitura[0].percentual, null);
});

test('estratégico: séries mensais só com o que ocupa horário, dentro da janela', () => {
  const meses = ['2026-08', '2026-09', '2026-10'];
  const r = resumirEstrategico({
    reservas: [
      { inicio: '2026-10-14T14:00:00', status: 'APROVADA', areaComumNome: 'Piscina' },
      { inicio: '2026-10-20T14:00:00', status: 'PENDENTE', areaComumNome: 'Piscina' },
      { inicio: '2026-09-02T10:00:00', status: 'CONCLUIDA', areaComumNome: 'Salão' },
      { inicio: '2026-10-21T10:00:00', status: 'CANCELADA', areaComumNome: 'Salão' },
      { inicio: '2026-05-01T10:00:00', status: 'APROVADA', areaComumNome: 'Salão' },
    ],
    reclamacoes: [
      { status: 'RESOLVIDO', category: 'Barulho', createdAt: '2026-09-01T12:00:00Z', updatedAt: '2026-09-03T12:00:00Z' },
      { status: 'RESOLVIDO', category: 'Barulho', createdAt: '2026-10-01T12:00:00Z', updatedAt: '2026-10-02T12:00:00Z' },
      { status: 'PENDENTE', category: 'Limpeza', createdAt: '2026-10-05T12:00:00Z', updatedAt: '2026-10-05T12:00:00Z' },
    ],
    kpisPorMes: {
      '2026-08': { total: 10, emAtraso: 1, pagasCentavos: 900 },
      '2026-09': null,
      '2026-10': { total: 0, emAtraso: 0, pagasCentavos: 0 },
    },
    multas: [
      { criadoEm: '2026-10-02T15:00:00Z', status: 'APLICADA', valorCentavos: 15000 },
      { criadoEm: '2026-10-03T15:00:00Z', status: 'CANCELADA', valorCentavos: 9000 },
    ],
  }, meses);

  assert.deepEqual(r.reservas.porMes.map((m) => m.total), [0, 1, 2]);
  assert.deepEqual(r.reservas.porArea, [{ area: 'Piscina', total: 2 }, { area: 'Salão', total: 1 }]);

  assert.deepEqual(r.ocorrencias.porMes.map((m) => [m.abertas, m.resolvidas]), [[0, 0], [1, 1], [2, 1]]);
  assert.deepEqual(r.ocorrencias.porCategoria[0], { categoria: 'Barulho', total: 2 });
  assert.equal(r.ocorrencias.tempoMedioResolucaoDias, 1.5, '2 dias e 1 dia');

  assert.equal(r.inadimplencia.porMes[0].taxa, 10);
  assert.equal(r.inadimplencia.porMes[1].taxa, null, 'mês sem resposta fica nulo');
  assert.equal(r.inadimplencia.porMes[2].taxa, null, 'sem fatura não há taxa');

  assert.deepEqual(r.multas.porMes[2], { mes: '2026-10', aplicadas: 2, canceladas: 1, valorCentavos: 15000 });
});
