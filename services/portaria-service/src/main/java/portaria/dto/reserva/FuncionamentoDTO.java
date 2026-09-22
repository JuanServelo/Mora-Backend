package portaria.dto.reserva;

import portaria.model.enums.ModoFuncionamento;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Funcionamento da área comum (RN-09). Usado na leitura e na gravação —
 * a agenda do frontend também consome isto para esmaecer as horas fechadas.
 */
public record FuncionamentoDTO(
        ModoFuncionamento modo,
        boolean aRevisar,
        List<HorarioDia> horarios
) {
    public record HorarioDia(
            DayOfWeek diaSemana,
            boolean fechado,
            LocalTime abertura,
            LocalTime fechamento
    ) {}
}
