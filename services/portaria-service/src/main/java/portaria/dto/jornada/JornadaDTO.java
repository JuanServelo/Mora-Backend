package portaria.dto.jornada;

import portaria.model.enums.SituacaoFuncional;
import portaria.model.enums.TipoJornada;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Jornada na leitura e na gravação. `dias` serve aos dois tipos: na semanal
 * vale `diaSemana`, na escala vale `posicao`.
 */
public record JornadaDTO(
        String id,
        String authUserId,
        String nome,
        SituacaoFuncional situacao,
        TipoJornada tipoJornada,
        LocalDate cicloInicio,
        Integer cicloTamanho,
        String observacoes,
        List<DiaDTO> dias
) {
    public record DiaDTO(
            DayOfWeek diaSemana,
            Integer posicao,
            boolean folga,
            LocalTime inicio,
            LocalTime fim
    ) {}
}
