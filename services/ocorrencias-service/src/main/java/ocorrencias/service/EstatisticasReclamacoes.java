package ocorrencias.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Monta a resposta das estatísticas a partir das linhas agregadas no banco.
 *
 * Funções puras: são elas que os testes exercitam, sem banco. O formato é o
 * mesmo que o auth-api devolvia, para o gestao-geral trocar de fonte sem
 * mudar de contrato.
 */
public final class EstatisticasReclamacoes {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    public static final int MESES_PADRAO = 6;
    private static final int MESES_MAX = 24;

    private EstatisticasReclamacoes() {}

    /**
     * Os últimos `n` meses, do mais antigo ao atual, como "YYYY-MM".
     *
     * Valor ausente ou absurdo vira o padrão ou o limite: vem da query string,
     * e um número enorme viraria uma série de milhares de pontos.
     */
    public static List<String> mesesDaJanela(Integer meses, LocalDate hoje) {
        int n = meses == null ? MESES_PADRAO : Math.min(MESES_MAX, Math.max(1, meses));
        YearMonth atual = YearMonth.from(hoje);
        List<String> lista = new ArrayList<>(n);
        for (int i = n - 1; i >= 0; i--) {
            lista.add(atual.minusMonths(i).toString());
        }
        return lista;
    }

    public record Atual(long abertas, long pendentes, long emAnalise, LocalDateTime maisAntigaDesde) {}

    public record Mes(String mes, long abertas, long resolvidas) {}

    public record Categoria(String categoria, long total) {}

    public record Resultado(
            List<String> meses,
            Atual atual,
            List<Mes> porMes,
            List<Categoria> porCategoria,
            Double tempoMedioResolucaoDias
    ) {}

    /** Mês sem linha vira zero, para o gráfico não ficar com buracos. */
    public static Resultado montar(List<String> meses,
                                   long pendentes,
                                   long emAnalise,
                                   LocalDateTime maisAntigaDesde,
                                   Map<String, Long> abertasPorMes,
                                   Map<String, Long> resolvidasPorMes,
                                   Map<String, Long> porCategoria,
                                   Double tempoMedioDias) {
        List<Mes> porMes = meses.stream()
                .map(m -> new Mes(m, abertasPorMes.getOrDefault(m, 0L), resolvidasPorMes.getOrDefault(m, 0L)))
                .toList();

        List<Categoria> categorias = porCategoria.entrySet().stream()
                .map(e -> new Categoria(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(Categoria::total).reversed()
                        .thenComparing(Categoria::categoria))
                .toList();

        Double media = tempoMedioDias == null || tempoMedioDias.isNaN()
                ? null
                : BigDecimal.valueOf(tempoMedioDias).setScale(1, RoundingMode.HALF_UP).doubleValue();

        return new Resultado(
                meses,
                new Atual(pendentes + emAnalise, pendentes, emAnalise, maisAntigaDesde),
                porMes,
                categorias,
                media
        );
    }

    /** Contagem por status: o que o painel do Admin Geral mostra. */
    public record Contagem(long total, long abertas, Map<String, Long> porStatus) {}

    public static Contagem contagem(Map<String, Long> porStatus) {
        long total = porStatus.values().stream().mapToLong(Long::longValue).sum();
        long abertas = porStatus.getOrDefault("PENDENTE", 0L) + porStatus.getOrDefault("EM_ANALISE", 0L);
        return new Contagem(total, abertas, porStatus);
    }
}
