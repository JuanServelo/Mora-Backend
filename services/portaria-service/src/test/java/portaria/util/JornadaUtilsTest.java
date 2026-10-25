package portaria.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import portaria.model.FuncionarioJornada;
import portaria.model.TurnoDia;
import portaria.model.enums.TipoJornada;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Posição no ciclo da escala (RF-09).
 *
 * A conta é `(dias % tamanho + tamanho) % tamanho + 1`, e o `+ tamanho` existe
 * por um motivo específico: em Java o resto de um número negativo é negativo,
 * então uma data anterior ao início do ciclo produziria posição zero ou
 * negativa e o turno sairia errado — ou estouraria o índice. É o caso que a
 * implementação ingênua erra, e por isso ele é testado explicitamente.
 */
class JornadaUtilsTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 10, 1);

    @Test
    @DisplayName("12x36: alterna trabalho e folga a cada dia")
    void ciclo12x36() {
        assertEquals(1, JornadaUtils.posicaoNoCiclo(INICIO, INICIO, 2));
        assertEquals(2, JornadaUtils.posicaoNoCiclo(INICIO.plusDays(1), INICIO, 2));
        assertEquals(1, JornadaUtils.posicaoNoCiclo(INICIO.plusDays(2), INICIO, 2));
        assertEquals(2, JornadaUtils.posicaoNoCiclo(INICIO.plusDays(3), INICIO, 2));
    }

    @Test
    @DisplayName("24x48: ciclo de três dias fecha certo")
    void ciclo24x48() {
        assertEquals(1, JornadaUtils.posicaoNoCiclo(INICIO, INICIO, 3));
        assertEquals(2, JornadaUtils.posicaoNoCiclo(INICIO.plusDays(1), INICIO, 3));
        assertEquals(3, JornadaUtils.posicaoNoCiclo(INICIO.plusDays(2), INICIO, 3));
        assertEquals(1, JornadaUtils.posicaoNoCiclo(INICIO.plusDays(3), INICIO, 3));
    }

    @Test
    @DisplayName("data anterior ao início do ciclo não produz posição negativa")
    void dataAnteriorAoInicio() {
        for (int i = 1; i <= 10; i++) {
            int pos = JornadaUtils.posicaoNoCiclo(INICIO.minusDays(i), INICIO, 3);
            assertTrue(pos >= 1 && pos <= 3,
                    "posição fora da faixa para " + i + " dias antes do início: " + pos);
        }
    }

    @Test
    @DisplayName("um dia antes do início é a última posição do ciclo")
    void vesperaDoInicio() {
        assertEquals(3, JornadaUtils.posicaoNoCiclo(INICIO.minusDays(1), INICIO, 3));
        assertEquals(2, JornadaUtils.posicaoNoCiclo(INICIO.minusDays(1), INICIO, 2));
    }

    @Test
    @DisplayName("ciclo longo (6x1) percorre as sete posições e reinicia")
    void ciclo6x1() {
        for (int i = 0; i < 7; i++) {
            assertEquals(i + 1, JornadaUtils.posicaoNoCiclo(INICIO.plusDays(i), INICIO, 7));
        }
        assertEquals(1, JornadaUtils.posicaoNoCiclo(INICIO.plusDays(7), INICIO, 7));
    }

    @Test
    @DisplayName("a posição nunca sai da faixa, em qualquer distância")
    void sempreNaFaixa() {
        for (int tamanho : new int[]{2, 3, 7}) {
            for (int d = -400; d <= 400; d += 7) {
                int pos = JornadaUtils.posicaoNoCiclo(INICIO.plusDays(d), INICIO, tamanho);
                assertTrue(pos >= 1 && pos <= tamanho,
                        "tamanho=" + tamanho + " offset=" + d + " posição=" + pos);
            }
        }
    }

    @Test
    @DisplayName("a tolerância de virada de turno é de 15 minutos")
    void tolerancia() {
        assertEquals(15, JornadaUtils.TOLERANCIA.toMinutes(),
                "o porteiro que chega 10 min antes não pode ser barrado");
    }

    // ── montagem ────────────────────────────────────────────────────────────

    private static TurnoDia diaDeCiclo(int posicao, String inicio, String fim) {
        TurnoDia d = new TurnoDia();
        d.setPosicao(posicao);
        d.setFolga(inicio == null);
        if (inicio != null) {
            d.setInicio(LocalTime.parse(inicio));
            d.setFim(LocalTime.parse(fim));
        }
        return d;
    }

    private static TurnoDia diaDaSemana(DayOfWeek dow, String inicio, String fim) {
        TurnoDia d = new TurnoDia();
        d.setDiaSemana(dow);
        d.setFolga(inicio == null);
        if (inicio != null) {
            d.setInicio(LocalTime.parse(inicio));
            d.setFim(LocalTime.parse(fim));
        }
        return d;
    }

    private static FuncionarioJornada porCiclo(int tamanho) {
        FuncionarioJornada j = new FuncionarioJornada();
        j.setTipoJornada(TipoJornada.ESCALA);
        j.setCicloInicio(INICIO);
        j.setCicloTamanho(tamanho);
        return j;
    }

    private static FuncionarioJornada semanal() {
        FuncionarioJornada j = new FuncionarioJornada();
        j.setTipoJornada(TipoJornada.SEMANAL_FIXA);
        return j;
    }

    // ── turno do dia ────────────────────────────────────────────────────────

    @Test
    @DisplayName("ciclo: o dia de trabalho e o de folga alternam conforme a posição")
    void turnoDoDiaPorCiclo() {
        var jornada = porCiclo(2);
        var dias = List.of(diaDeCiclo(1, "07:00", "19:00"), diaDeCiclo(2, null, null));

        assertFalse(JornadaUtils.turnoDoDia(jornada, dias, INICIO).orElseThrow().isFolga(),
                "o primeiro dia do ciclo 12x36 é de trabalho");
        assertTrue(JornadaUtils.turnoDoDia(jornada, dias, INICIO.plusDays(1)).orElseThrow().isFolga(),
                "o segundo dia do ciclo 12x36 é folga");
    }

    @Test
    @DisplayName("semanal fixa: o turno vem do dia da semana, não do ciclo")
    void turnoDoDiaSemanal() {
        var jornada = semanal();
        // 2026-10-01 é uma quinta-feira.
        var dias = List.of(diaDaSemana(DayOfWeek.THURSDAY, "08:00", "17:00"));

        assertTrue(JornadaUtils.turnoDoDia(jornada, dias, INICIO).isPresent());
        assertTrue(JornadaUtils.turnoDoDia(jornada, dias, INICIO.plusDays(1)).isEmpty(),
                "sexta não tem turno cadastrado");
    }

    @Test
    @DisplayName("ciclo sem configuração não resolve turno, em vez de estourar")
    void cicloIncompleto() {
        FuncionarioJornada j = new FuncionarioJornada();
        j.setTipoJornada(TipoJornada.ESCALA);
        assertTrue(JornadaUtils.turnoDoDia(j, List.of(diaDeCiclo(1, "07:00", "19:00")), INICIO)
                .isEmpty());
    }

    // ── janela ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("turno diurno produz janela no mesmo dia")
    void janelaDiurna() {
        var jornada = porCiclo(2);
        var dias = List.of(diaDeCiclo(1, "07:00", "19:00"), diaDeCiclo(2, null, null));

        var janela = JornadaUtils.janelaDoDia(jornada, dias, INICIO);
        assertNotNull(janela);
        assertEquals(INICIO.atTime(7, 0), janela.inicio());
        assertEquals(INICIO.atTime(19, 0), janela.fim());
        assertTrue(janela.descricao().contains("07:00"));
    }

    @Test
    @DisplayName("turno noturno termina no dia seguinte")
    void janelaNoturna() {
        var jornada = porCiclo(2);
        var dias = List.of(diaDeCiclo(1, "19:00", "07:00"), diaDeCiclo(2, null, null));

        var janela = JornadaUtils.janelaDoDia(jornada, dias, INICIO);
        assertNotNull(janela);
        assertEquals(INICIO.plusDays(1).atTime(7, 0), janela.fim(),
                "19:00 às 07:00 atravessa a meia-noite");
    }

    @Test
    @DisplayName("dia de folga não tem janela")
    void folgaSemJanela() {
        var jornada = porCiclo(2);
        var dias = List.of(diaDeCiclo(1, "07:00", "19:00"), diaDeCiclo(2, null, null));
        assertNull(JornadaUtils.janelaDoDia(jornada, dias, INICIO.plusDays(1)));
    }

    // ── turno vigente ───────────────────────────────────────────────────────

    @Test
    @DisplayName("dentro do turno diurno, está vigente")
    void vigenteDiurno() {
        var jornada = porCiclo(2);
        var dias = List.of(diaDeCiclo(1, "07:00", "19:00"), diaDeCiclo(2, null, null));

        assertTrue(JornadaUtils.turnoVigente(jornada, dias, INICIO.atTime(12, 0)).isPresent());
    }

    @Test
    @DisplayName("às 2h da manhã, quem governa é o turno noturno do dia ANTERIOR")
    void vigenteNaMadrugada() {
        var jornada = porCiclo(2);
        var dias = List.of(diaDeCiclo(1, "19:00", "07:00"), diaDeCiclo(2, null, null));

        var vigente = JornadaUtils.turnoVigente(jornada, dias, INICIO.plusDays(1).atTime(2, 0));
        assertTrue(vigente.isPresent(),
                "sem olhar o dia anterior, a madrugada cairia na folga e a entrada seria barrada");
        assertEquals(INICIO, vigente.orElseThrow().diaOrigem());
    }

    @Test
    @DisplayName("chegar 10 min antes cabe na tolerância de 15")
    void toleranciaAntes() {
        var jornada = porCiclo(2);
        var dias = List.of(diaDeCiclo(1, "07:00", "19:00"), diaDeCiclo(2, null, null));

        assertTrue(JornadaUtils.turnoVigente(jornada, dias, INICIO.atTime(6, 50)).isPresent());
    }

    @Test
    @DisplayName("chegar 30 min antes está fora do turno")
    void foraDaTolerancia() {
        var jornada = porCiclo(2);
        var dias = List.of(diaDeCiclo(1, "07:00", "19:00"), diaDeCiclo(2, null, null));

        assertTrue(JornadaUtils.turnoVigente(jornada, dias, INICIO.atTime(6, 30)).isEmpty());
    }

    @Test
    @DisplayName("sair 10 min depois do fim ainda é o mesmo turno")
    void toleranciaDepois() {
        var jornada = porCiclo(2);
        var dias = List.of(diaDeCiclo(1, "07:00", "19:00"), diaDeCiclo(2, null, null));

        assertTrue(JornadaUtils.turnoVigente(jornada, dias, INICIO.atTime(19, 10)).isPresent());
    }

    @Test
    @DisplayName("no dia de folga não há turno vigente")
    void folgaSemVigente() {
        var jornada = porCiclo(2);
        var dias = List.of(diaDeCiclo(1, "07:00", "19:00"), diaDeCiclo(2, null, null));

        assertTrue(JornadaUtils.turnoVigente(jornada, dias, INICIO.plusDays(1).atTime(12, 0))
                .isEmpty());
    }

    @Test
    @DisplayName("candidatas sempre consideram o dia anterior e o corrente")
    void candidatas() {
        var jornada = porCiclo(1);
        var dias = List.of(diaDeCiclo(1, "19:00", "07:00"));
        assertEquals(2, JornadaUtils.candidatas(jornada, dias, INICIO.atTime(2, 0)).size());
    }

    @Test
    @DisplayName("descricaoDoDia responde mesmo em dia de folga")
    void descricaoDoDia() {
        var jornada = porCiclo(2);
        var dias = List.of(diaDeCiclo(1, "07:00", "19:00"), diaDeCiclo(2, null, null));

        assertNotNull(JornadaUtils.descricaoDoDia(jornada, dias, INICIO));
        assertNotNull(JornadaUtils.descricaoDoDia(jornada, dias, INICIO.plusDays(1)));
    }

    @Test
    @DisplayName("proximosPlantoes devolve os turnos à frente, sem as folgas")
    void proximosPlantoes() {
        var jornada = porCiclo(2);
        var dias = List.of(diaDeCiclo(1, "07:00", "19:00"), diaDeCiclo(2, null, null));

        var proximos = JornadaUtils.proximosPlantoes(jornada, dias, INICIO, 4);
        assertFalse(proximos.isEmpty());
        assertTrue(proximos.size() <= 4);
    }
}
