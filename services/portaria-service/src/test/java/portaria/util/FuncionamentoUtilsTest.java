package portaria.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import portaria.model.AreaComumHorario;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Janelas de funcionamento do espaço.
 *
 * O caso que justifica a classe é a janela que atravessa a meia-noite: um salão
 * que vai das 18:00 às 02:00 só é representável somando um dia ao fechamento, e
 * uma reserva à 01:00 de domingo é governada pelo horário de SÁBADO. É o erro
 * que a implementação ingênua comete, e por isso ele lidera os testes.
 */
class FuncionamentoUtilsTest {

    private static AreaComumHorario horario(DayOfWeek dia, String abre, String fecha) {
        AreaComumHorario h = new AreaComumHorario();
        h.setDiaSemana(dia);
        h.setAbertura(LocalTime.parse(abre));
        h.setFechamento(LocalTime.parse(fecha));
        h.setFechado(false);
        return h;
    }

    private static AreaComumHorario fechado(DayOfWeek dia) {
        AreaComumHorario h = new AreaComumHorario();
        h.setDiaSemana(dia);
        h.setFechado(true);
        return h;
    }

    // 2026-10-24 é um sábado; 25 é domingo.
    private static final java.time.LocalDate SABADO = java.time.LocalDate.of(2026, 10, 24);

    @Test
    @DisplayName("horário comum: intervalo dentro da janela é permitido")
    void dentroDaJanela() {
        var horarios = List.of(horario(DayOfWeek.SATURDAY, "08:00", "20:00"));
        assertTrue(FuncionamentoUtils.permitidoInicioEFim(horarios,
                SABADO.atTime(10, 0), SABADO.atTime(12, 0)));
    }

    @Test
    @DisplayName("começar antes da abertura é recusado")
    void antesDaAbertura() {
        var horarios = List.of(horario(DayOfWeek.SATURDAY, "08:00", "20:00"));
        assertFalse(FuncionamentoUtils.permitidoInicioEFim(horarios,
                SABADO.atTime(7, 0), SABADO.atTime(9, 0)));
    }

    @Test
    @DisplayName("terminar exatamente no fechamento vale; começar no fechamento não")
    void bordaDoFechamento() {
        var horarios = List.of(horario(DayOfWeek.SATURDAY, "08:00", "20:00"));
        assertTrue(FuncionamentoUtils.permitidoInicioEFim(horarios,
                SABADO.atTime(18, 0), SABADO.atTime(20, 0)),
                "terminar às 20:00 num espaço que fecha às 20:00 é válido");
        assertFalse(FuncionamentoUtils.permitidoInicioEFim(horarios,
                SABADO.atTime(20, 0), SABADO.atTime(21, 0)),
                "começar às 20:00 já é fora do funcionamento");
    }

    @Test
    @DisplayName("janela que atravessa a meia-noite cobre a madrugada do dia seguinte")
    void atravessaMeiaNoite() {
        var horarios = List.of(horario(DayOfWeek.SATURDAY, "18:00", "02:00"));
        // 01:00 de DOMINGO é governado pelo horário de sábado.
        assertTrue(FuncionamentoUtils.permitidoInicioEFim(horarios,
                SABADO.plusDays(1).atTime(0, 30), SABADO.plusDays(1).atTime(1, 30)),
                "a janela do dia anterior precisa entrar nas candidatas");
    }

    @Test
    @DisplayName("reserva que cruza a meia-noite dentro da mesma janela é permitida")
    void cruzaMeiaNoiteNaMesmaJanela() {
        var horarios = List.of(horario(DayOfWeek.SATURDAY, "18:00", "02:00"));
        assertTrue(FuncionamentoUtils.permitidoInicioEFim(horarios,
                SABADO.atTime(23, 0), SABADO.plusDays(1).atTime(1, 0)));
    }

    @Test
    @DisplayName("madrugada além do fechamento é recusada")
    void madrugadaAlemDoFechamento() {
        var horarios = List.of(horario(DayOfWeek.SATURDAY, "18:00", "02:00"));
        assertFalse(FuncionamentoUtils.permitidoInicioEFim(horarios,
                SABADO.plusDays(1).atTime(3, 0), SABADO.plusDays(1).atTime(4, 0)));
    }

    @Test
    @DisplayName("dia marcado como fechado não permite nada")
    void diaFechado() {
        var horarios = List.of(fechado(DayOfWeek.SATURDAY));
        assertFalse(FuncionamentoUtils.permitidoInicioEFim(horarios,
                SABADO.atTime(10, 0), SABADO.atTime(12, 0)));
    }

    @Test
    @DisplayName("sem horário cadastrado para o dia, nada é permitido naquele dia")
    void semHorarioNoDia() {
        var horarios = List.of(horario(DayOfWeek.MONDAY, "08:00", "20:00"));
        assertFalse(FuncionamentoUtils.permitidoInicioEFim(horarios,
                SABADO.atTime(10, 0), SABADO.atTime(12, 0)));
    }

    @Test
    @DisplayName("candidatas sempre incluem o dia anterior")
    void candidatasIncluemDiaAnterior() {
        var horarios = List.of(
                horario(DayOfWeek.FRIDAY, "18:00", "02:00"),
                horario(DayOfWeek.SATURDAY, "08:00", "20:00"));
        var janelas = FuncionamentoUtils.candidatas(horarios, SABADO.atTime(1, 0));
        assertEquals(2, janelas.size(), "sexta e sábado precisam ser avaliados");
    }

    @Test
    @DisplayName("mensagem de recusa nomeia o espaço, para a pessoa saber do que se trata")
    void mensagemNomeiaEspaco() {
        var horarios = List.of(horario(DayOfWeek.SATURDAY, "08:00", "20:00"));
        String msg = FuncionamentoUtils.mensagemRecusa(horarios, "Salão de Festas",
                SABADO.atTime(7, 0), SABADO.atTime(9, 0));
        assertNotNull(msg);
        assertTrue(msg.contains("Salão de Festas"), "mensagem recebida: " + msg);
    }

    @Test
    @DisplayName("a janela expõe suas bordas e sabe dizer o que contém")
    void janelaExpoeBordas() {
        var h = horario(DayOfWeek.SATURDAY, "18:00", "02:00");
        var janela = FuncionamentoUtils.janelaDoDia(h, SABADO);

        assertEquals(SABADO.atTime(18, 0), janela.inicio());
        assertEquals(SABADO.plusDays(1).atTime(2, 0), janela.fim(),
                "o fechamento menor que a abertura cai no dia seguinte");
        assertEquals(DayOfWeek.SATURDAY, janela.diaOrigem());

        assertTrue(janela.contem(SABADO.atTime(20, 0), SABADO.atTime(22, 0)));
        assertFalse(janela.contem(SABADO.atTime(17, 0), SABADO.atTime(19, 0)),
                "começar antes da abertura não está contido");
        assertFalse(janela.contem(SABADO.atTime(23, 0), SABADO.plusDays(1).atTime(3, 0)),
                "terminar depois do fechamento não está contido");
    }

    @Test
    @DisplayName("dia fechado ou sem horário não produz janela")
    void semJanela() {
        assertNull(FuncionamentoUtils.janelaDoDia(fechado(DayOfWeek.SATURDAY), SABADO));
        assertNull(FuncionamentoUtils.janelaDoDia(null, SABADO));
    }

    @Test
    @DisplayName("nomeDia e hora formatam para leitura humana")
    void formatacao() {
        assertNotNull(FuncionamentoUtils.nomeDia(DayOfWeek.SATURDAY));
        assertEquals("20:00", FuncionamentoUtils.hora(LocalTime.of(20, 0)));
    }
}
