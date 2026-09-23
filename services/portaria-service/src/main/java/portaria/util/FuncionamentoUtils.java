package portaria.util;

import portaria.model.AreaComumHorario;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Janelas de funcionamento da área comum (RN-09).
 *
 * O ponto central é que a reserva precisa caber em UMA janela concreta, não na
 * união delas: validar contra a união deixaria passar uma reserva que atravessa
 * o período em que o espaço está fechado entre dois dias.
 *
 * O segundo ponto é que uma reserva de madrugada pertence à janela aberta no dia
 * ANTERIOR — domingo 00:30 é governado pela janela de sábado 10:00–02:00. Por
 * isso as candidatas sempre incluem o dia anterior ao início.
 */
public final class FuncionamentoUtils {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private static final Map<DayOfWeek, String> NOME_DIA = new EnumMap<>(DayOfWeek.class);
    static {
        NOME_DIA.put(DayOfWeek.MONDAY, "segunda-feira");
        NOME_DIA.put(DayOfWeek.TUESDAY, "terça-feira");
        NOME_DIA.put(DayOfWeek.WEDNESDAY, "quarta-feira");
        NOME_DIA.put(DayOfWeek.THURSDAY, "quinta-feira");
        NOME_DIA.put(DayOfWeek.FRIDAY, "sexta-feira");
        NOME_DIA.put(DayOfWeek.SATURDAY, "sábado");
        NOME_DIA.put(DayOfWeek.SUNDAY, "domingo");
    }

    private FuncionamentoUtils() {}

    public record Janela(LocalDateTime inicio, LocalDateTime fim, DayOfWeek diaOrigem) {
        public boolean contem(LocalDateTime i, LocalDateTime f) {
            return !i.isBefore(inicio) && !f.isAfter(fim);
        }
    }

    public static String nomeDia(DayOfWeek dia) {
        return NOME_DIA.getOrDefault(dia, dia.toString());
    }

    public static String hora(LocalTime t) {
        return t == null ? "—" : t.format(HORA);
    }

    /** Janela concreta daquele dia; vazia se fechado ou sem horários. */
    public static Janela janelaDoDia(AreaComumHorario h, LocalDate dia) {
        if (h == null || h.isFechado() || h.getAbertura() == null || h.getFechamento() == null) {
            return null;
        }
        LocalDateTime ini = dia.atTime(h.getAbertura());
        LocalDateTime fim = h.getFechamento().isAfter(h.getAbertura())
                ? dia.atTime(h.getFechamento())
                : dia.plusDays(1).atTime(h.getFechamento()); // atravessa a meia-noite
        return new Janela(ini, fim, h.getDiaSemana());
    }

    private static Map<DayOfWeek, AreaComumHorario> porDia(List<AreaComumHorario> horarios) {
        Map<DayOfWeek, AreaComumHorario> mapa = new EnumMap<>(DayOfWeek.class);
        for (AreaComumHorario h : horarios) mapa.put(h.getDiaSemana(), h);
        return mapa;
    }

    /**
     * Janelas que podem conter um intervalo iniciado em `inicio`.
     * Inclui a do dia anterior, que é quem governa as madrugadas.
     */
    public static List<Janela> candidatas(List<AreaComumHorario> horarios, LocalDateTime inicio) {
        Map<DayOfWeek, AreaComumHorario> mapa = porDia(horarios);
        List<Janela> janelas = new ArrayList<>();
        for (LocalDate dia : List.of(inicio.toLocalDate().minusDays(1), inicio.toLocalDate())) {
            Janela j = janelaDoDia(mapa.get(dia.getDayOfWeek()), dia);
            if (j != null) janelas.add(j);
        }
        return janelas;
    }

    /** Uma única janela precisa conter o intervalo inteiro. */
    public static boolean permitido(List<AreaComumHorario> horarios,
                                    LocalDateTime inicio, LocalDateTime fim) {
        return candidatas(horarios, inicio).stream().anyMatch(j -> j.contem(inicio, fim));
    }

    /**
     * Instante coberto por alguma janela.
     *
     * O fechamento é inclusivo só para o fim da reserva: terminar exatamente às
     * 20:00 num espaço que fecha às 20:00 é válido, mas começar às 20:00 não.
     */
    private static boolean instanteEm(List<Janela> janelas, LocalDateTime t, boolean fechamentoInclusivo) {
        return janelas.stream().anyMatch(j -> fechamentoInclusivo
                ? (!t.isBefore(j.inicio()) && !t.isAfter(j.fim()))
                : (!t.isBefore(j.inicio()) && t.isBefore(j.fim())));
    }

    public static boolean inicioValido(List<AreaComumHorario> horarios, LocalDateTime inicio) {
        return instanteEm(candidatas(horarios, inicio), inicio, false);
    }

    public static boolean fimValido(List<AreaComumHorario> horarios, LocalDateTime fim) {
        return instanteEm(candidatas(horarios, fim), fim, true);
    }

    /**
     * Regra vigente: só o horário de início e o de término precisam cair dentro
     * do funcionamento. Uma reserva de período corrido ocupa o espaço de forma
     * contínua, inclusive nas faixas em que ele estaria fechado, então exigir
     * que o intervalo inteiro coubesse numa janela inviabilizaria o caso.
     */
    public static boolean permitidoInicioEFim(List<AreaComumHorario> horarios,
                                              LocalDateTime inicio, LocalDateTime fim) {
        return inicioValido(horarios, inicio) && fimValido(horarios, fim);
    }

    /** Diz qual das duas pontas está fora do funcionamento (RN-09). */
    public static String mensagemRecusa(List<AreaComumHorario> horarios, String nomeEspaco,
                                        LocalDateTime inicio, LocalDateTime fim) {
        if (!inicioValido(horarios, inicio)) {
            return descreverDia(horarios, nomeEspaco, inicio,
                    "Selecione um horário de início dentro desse intervalo.");
        }
        return descreverDia(horarios, nomeEspaco, fim,
                "O término da reserva precisa cair dentro desse intervalo.");
    }

    private static String descreverDia(List<AreaComumHorario> horarios, String nomeEspaco,
                                       LocalDateTime instante, String complemento) {
        Map<DayOfWeek, AreaComumHorario> mapa = porDia(horarios);
        AreaComumHorario h = mapa.get(instante.getDayOfWeek());
        if (h == null || h.isFechado()) {
            return "O espaço " + nomeEspaco + " não abre " + nomeDia(instante.getDayOfWeek()) + ".";
        }
        return "O espaço " + nomeEspaco + " funciona " + nomeDia(instante.getDayOfWeek())
                + " das " + hora(h.getAbertura()) + " às " + hora(h.getFechamento())
                + ". " + complemento;
    }
}
