import { describe, it, expect } from 'vitest';
import {
  mesesDaJanela,
  montarEstatisticasReclamacoes,
  OCORRENCIAS_MESES_PADRAO,
} from '../services/estatisticasService.js';

describe('mesesDaJanela', () => {
  const agora = new Date('2026-10-14T15:00:00Z');

  it('devolve os últimos n meses, do mais antigo ao atual', () => {
    expect(mesesDaJanela(3, agora)).toEqual(['2026-08', '2026-09', '2026-10']);
  });

  it('atravessa a virada do ano', () => {
    expect(mesesDaJanela(3, new Date('2026-01-10T12:00:00Z'))).toEqual(['2025-11', '2025-12', '2026-01']);
  });

  it('usa o mês de São Paulo, não o de UTC', () => {
    // 1º de novembro 01h UTC ainda é 31 de outubro 22h em São Paulo.
    expect(mesesDaJanela(1, new Date('2026-11-01T01:00:00Z'))).toEqual(['2026-10']);
  });

  it('valor ausente ou inválido cai no padrão; exagero é limitado', () => {
    expect(mesesDaJanela(undefined, agora)).toHaveLength(OCORRENCIAS_MESES_PADRAO);
    expect(mesesDaJanela('abc', agora)).toHaveLength(OCORRENCIAS_MESES_PADRAO);
    expect(mesesDaJanela(9999, agora)).toHaveLength(24);
    expect(mesesDaJanela(-5, agora)).toHaveLength(1);
  });
});

describe('montarEstatisticasReclamacoes', () => {
  const meses = ['2026-08', '2026-09', '2026-10'];

  it('soma as abertas atuais e preenche os meses sem linha com zero', () => {
    // O pg devolve COUNT como texto; a montagem converte.
    const r = montarEstatisticasReclamacoes({
      meses,
      atual: { pendentes: '2', emAnalise: '1', maisAntigaDesde: '2026-08-03T10:00:00.000Z' },
      abertasPorMes: [{ mes: '2026-08', total: '4' }, { mes: '2026-10', total: '1' }],
      resolvidasPorMes: [{ mes: '2026-09', total: '3' }],
      porCategoria: [{ categoria: 'Barulho', total: '1' }, { categoria: 'Limpeza', total: '4' }],
      tempoMedioDias: '2.4666',
    });

    expect(r.atual).toEqual({ abertas: 3, pendentes: 2, emAnalise: 1, maisAntigaDesde: '2026-08-03T10:00:00.000Z' });
    expect(r.porMes).toEqual([
      { mes: '2026-08', abertas: 4, resolvidas: 0 },
      { mes: '2026-09', abertas: 0, resolvidas: 3 },
      { mes: '2026-10', abertas: 1, resolvidas: 0 },
    ]);
    expect(r.porCategoria).toEqual([
      { categoria: 'Limpeza', total: 4 },
      { categoria: 'Barulho', total: 1 },
    ]);
    expect(r.tempoMedioResolucaoDias).toBe(2.5);
  });

  it('sem nenhuma ocorrência, zera tudo e não inventa tempo médio', () => {
    const r = montarEstatisticasReclamacoes({
      meses,
      atual: { pendentes: '0', emAnalise: '0', maisAntigaDesde: null },
      abertasPorMes: [],
      resolvidasPorMes: [],
      porCategoria: [],
      tempoMedioDias: null,
    });

    expect(r.atual.abertas).toBe(0);
    expect(r.atual.maisAntigaDesde).toBeNull();
    expect(r.porMes.every((m) => m.abertas === 0 && m.resolvidas === 0)).toBe(true);
    expect(r.tempoMedioResolucaoDias).toBeNull();
  });
});
