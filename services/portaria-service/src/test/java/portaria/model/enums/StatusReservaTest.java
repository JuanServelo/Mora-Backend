package portaria.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * As transições de status são a regra que impede uma reserva recusada voltar a
 * valer por requisição direta, e `ocupaHorario` é o que decide se um horário
 * está livre. Os dois são consultados de vários pontos — service, repositório e
 * a constraint EXCLUDE da V4 repete o mesmo recorte no banco.
 */
class StatusReservaTest {

    @Test
    @DisplayName("pendente pode ser aprovada, recusada, expirada ou cancelada")
    void dePendente() {
        assertTrue(StatusReserva.PENDENTE.podeIrPara(StatusReserva.APROVADA));
        assertTrue(StatusReserva.PENDENTE.podeIrPara(StatusReserva.RECUSADA));
        assertTrue(StatusReserva.PENDENTE.podeIrPara(StatusReserva.EXPIRADA));
        assertTrue(StatusReserva.PENDENTE.podeIrPara(StatusReserva.CANCELADA));
    }

    @Test
    @DisplayName("aprovada só pode ser cancelada ou concluída")
    void deAprovada() {
        assertTrue(StatusReserva.APROVADA.podeIrPara(StatusReserva.CANCELADA));
        assertTrue(StatusReserva.APROVADA.podeIrPara(StatusReserva.CONCLUIDA));
        assertFalse(StatusReserva.APROVADA.podeIrPara(StatusReserva.PENDENTE),
                "aprovar e depois voltar a pendente desfaria a decisão do síndico");
        assertFalse(StatusReserva.APROVADA.podeIrPara(StatusReserva.RECUSADA));
    }

    @ParameterizedTest
    @EnumSource(value = StatusReserva.class,
            names = {"RECUSADA", "EXPIRADA", "CANCELADA", "CONCLUIDA"})
    @DisplayName("estado final não vai para lugar nenhum")
    void estadosFinais(StatusReserva finalizado) {
        for (StatusReserva destino : StatusReserva.values()) {
            assertFalse(finalizado.podeIrPara(destino),
                    finalizado + " não deveria aceitar virar " + destino);
        }
    }

    @Test
    @DisplayName("recusada não volta a ser aprovada — o caso que a guarda existe para impedir")
    void recusadaNaoVolta() {
        assertFalse(StatusReserva.RECUSADA.podeIrPara(StatusReserva.APROVADA));
    }

    @Test
    @DisplayName("nenhum status transiciona para si mesmo")
    void semAutoTransicao() {
        for (StatusReserva s : StatusReserva.values()) {
            assertFalse(s.podeIrPara(s), s + " não deveria transicionar para si mesmo");
        }
    }

    @Test
    @DisplayName("ocupam o horário: pendente, aprovada e concluída")
    void ocupamHorario() {
        assertTrue(StatusReserva.PENDENTE.ocupaHorario(),
                "pendente bloqueia: senão duas pessoas disputam o mesmo horário");
        assertTrue(StatusReserva.APROVADA.ocupaHorario());
        assertTrue(StatusReserva.CONCLUIDA.ocupaHorario());
    }

    @Test
    @DisplayName("liberam o horário: cancelada, recusada e expirada")
    void liberamHorario() {
        assertFalse(StatusReserva.CANCELADA.ocupaHorario());
        assertFalse(StatusReserva.RECUSADA.ocupaHorario());
        assertFalse(StatusReserva.EXPIRADA.ocupaHorario(),
                "expirada precisa liberar: é o que impede a pendente esquecida travar o espaço");
    }
}
