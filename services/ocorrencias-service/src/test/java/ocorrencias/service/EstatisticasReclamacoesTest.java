package ocorrencias.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EstatisticasReclamacoesTest {

    @Test
    void janelaTerminaNoMesCorrenteEAtravessaOAno() {
        assertEquals(List.of("2025-11", "2025-12", "2026-01"),
                EstatisticasReclamacoes.mesesDaJanela(3, LocalDate.of(2026, 1, 15)));
    }

    @Test
    void janelaUsaOPadraoSemValorERespeitaOsLimites() {
        LocalDate hoje = LocalDate.of(2026, 10, 20);
        assertEquals(6, EstatisticasReclamacoes.mesesDaJanela(null, hoje).size());
        assertEquals(1, EstatisticasReclamacoes.mesesDaJanela(0, hoje).size());
        assertEquals(24, EstatisticasReclamacoes.mesesDaJanela(500, hoje).size());
    }

    @Test
    void mesSemLinhaViraZeroECategoriasVemDaMaiorParaAMenor() {
        var r = EstatisticasReclamacoes.montar(
                List.of("2026-08", "2026-09", "2026-10"),
                1, 2, LocalDateTime.of(2026, 9, 1, 10, 0),
                Map.of("2026-09", 3L, "2026-10", 1L),
                Map.of("2026-10", 2L),
                Map.of("LIMPEZA", 1L, "BARULHO", 3L, "ANIMAIS", 1L),
                1.26);

        assertEquals(3, r.atual().abertas());
        assertEquals(List.of(0L, 3L, 1L), r.porMes().stream().map(EstatisticasReclamacoes.Mes::abertas).toList());
        assertEquals(List.of(0L, 0L, 2L), r.porMes().stream().map(EstatisticasReclamacoes.Mes::resolvidas).toList());
        assertEquals(List.of("BARULHO", "ANIMAIS", "LIMPEZA"),
                r.porCategoria().stream().map(EstatisticasReclamacoes.Categoria::categoria).toList(),
                "empate desfeito pelo nome");
        assertEquals(1.3, r.tempoMedioResolucaoDias());
    }

    @Test
    void semResolvidasOTempoMedioENulo() {
        var r = EstatisticasReclamacoes.montar(List.of("2026-10"), 0, 0, null,
                Map.of(), Map.of(), Map.of(), null);
        assertNull(r.tempoMedioResolucaoDias());
        assertNull(r.atual().maisAntigaDesde());
    }

    @Test
    void contagemSomaAbertasComoPendentesMaisEmAnalise() {
        var c = EstatisticasReclamacoes.contagem(Map.of("PENDENTE", 2L, "EM_ANALISE", 1L, "RESOLVIDO", 4L));
        assertEquals(7, c.total());
        assertEquals(3, c.abertas());
    }
}
