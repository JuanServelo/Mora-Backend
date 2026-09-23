package portaria.util;

import portaria.model.FuncionarioJornada;
import portaria.model.TurnoDia;
import portaria.model.enums.TipoJornada;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cálculo de turno (RN-03).
 *
 * Dois pontos concentram todo o risco desta regra:
 *
 * 1. A posição no ciclo é contada em DIAS DE CALENDÁRIO. Dividir diferença de
 *    timestamps por 86.400.000 erra em mudança de horário de verão, e o erro
 *    aparece como bloqueio indevido no portão. O módulo também é normalizado
 *    para positivo, senão datas anteriores ao início do ciclo dão posição
 *    negativa.
 *
 * 2. Um turno que vira a meia-noite continua vigente na madrugada, mesmo que o
 *    dia seguinte seja folga. Por isso as janelas candidatas sempre incluem o
 *    turno iniciado no dia ANTERIOR — checar só o dia corrente bloquearia o
 *    porteiro noturno à meia-noite, no meio do próprio plantão.
 */
public final class JornadaUtils {

    /** Tolerância de entrada, antes do início e depois do fim do turno. */
    public static final Duration TOLERANCIA = Duration.ofMinutes(15);

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private JornadaUtils() {}

    public record Janela(LocalDateTime inicio, LocalDateTime fim, LocalDate diaOrigem) {
        public boolean contem(LocalDateTime t, Duration tolerancia) {
            return !t.isBefore(inicio.minus(tolerancia)) && !t.isAfter(fim.plus(tolerancia));
        }
        public String descricao() {
            return inicio.format(HORA) + " às " + fim.format(HORA);
        }
    }

    /**
     * Posição do dia dentro do ciclo, de 1 a tamanho.
     * Dias de calendário e módulo positivo.
     */
    public static int posicaoNoCiclo(LocalDate dia, LocalDate cicloInicio, int cicloTamanho) {
        long dias = ChronoUnit.DAYS.between(cicloInicio, dia);
        int mod = (int) (((dias % cicloTamanho) + cicloTamanho) % cicloTamanho);
        return mod + 1;
    }

    /** Configuração daquele dia, conforme o tipo de jornada. */
    public static Optional<TurnoDia> turnoDoDia(FuncionarioJornada jornada,
                                                List<TurnoDia> dias, LocalDate dia) {
        if (jornada.getTipoJornada() == TipoJornada.SEMANAL_FIXA) {
            return dias.stream()
                    .filter(d -> d.getDiaSemana() == dia.getDayOfWeek())
                    .findFirst();
        }
        if (jornada.getCicloInicio() == null || jornada.getCicloTamanho() == null
                || jornada.getCicloTamanho() < 1) {
            return Optional.empty();
        }
        int posicao = posicaoNoCiclo(dia, jornada.getCicloInicio(), jornada.getCicloTamanho());
        return dias.stream()
                .filter(d -> d.getPosicao() != null && d.getPosicao() == posicao)
                .findFirst();
    }

    /** Janela concreta daquele dia; vazia se folga ou sem horários. */
    public static Janela janelaDoDia(FuncionarioJornada jornada, List<TurnoDia> dias, LocalDate dia) {
        TurnoDia t = turnoDoDia(jornada, dias, dia).orElse(null);
        if (t == null || t.isFolga() || t.getInicio() == null || t.getFim() == null) return null;
        LocalDateTime ini = dia.atTime(t.getInicio());
        LocalDateTime fim = t.getFim().isAfter(t.getInicio())
                ? dia.atTime(t.getFim())
                : dia.plusDays(1).atTime(t.getFim()); // vira a meia-noite
        return new Janela(ini, fim, dia);
    }

    /**
     * Janelas que podem conter o instante: a do próprio dia e a do dia anterior,
     * que é quem governa a madrugada.
     */
    public static List<Janela> candidatas(FuncionarioJornada jornada, List<TurnoDia> dias,
                                          LocalDateTime instante) {
        List<Janela> janelas = new ArrayList<>();
        for (LocalDate dia : List.of(instante.toLocalDate().minusDays(1), instante.toLocalDate())) {
            Janela j = janelaDoDia(jornada, dias, dia);
            if (j != null) janelas.add(j);
        }
        return janelas;
    }

    /** Turno vigente no instante, já com a tolerância aplicada. */
    public static Optional<Janela> turnoVigente(FuncionarioJornada jornada, List<TurnoDia> dias,
                                                LocalDateTime instante) {
        return candidatas(jornada, dias, instante).stream()
                .filter(j -> j.contem(instante, TOLERANCIA))
                .findFirst();
    }

    /** Turno previsto para o dia, para exibir e gravar como snapshot. */
    public static String descricaoDoDia(FuncionarioJornada jornada, List<TurnoDia> dias, LocalDate dia) {
        Janela j = janelaDoDia(jornada, dias, dia);
        if (j == null) return "Folga";
        return j.descricao();
    }

    /** Próximos dias de trabalho, para conferir se a escala ficou correta (RN-03). */
    public static List<ProximoPlantao> proximosPlantoes(FuncionarioJornada jornada,
                                                        List<TurnoDia> dias,
                                                        LocalDate inicio, int quantidadeDias) {
        List<ProximoPlantao> saida = new ArrayList<>();
        for (int i = 0; i < quantidadeDias; i++) {
            LocalDate dia = inicio.plusDays(i);
            Janela j = janelaDoDia(jornada, dias, dia);
            if (j != null) {
                saida.add(new ProximoPlantao(dia, j.inicio().toLocalTime(), j.fim().toLocalTime(),
                        j.fim().toLocalDate().isAfter(dia)));
            }
        }
        return saida;
    }

    public record ProximoPlantao(LocalDate dia, LocalTime inicio, LocalTime fim, boolean viraODia) {}
}
