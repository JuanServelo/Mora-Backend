package vagas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * DTO para o proprietário publicar um período de disponibilidade da sua vaga (RF10 US1).
 */
public record DisponibilidadeRequestDTO(

        @NotBlank(message = "ID da vaga é obrigatório")
        String vagaId,

        @NotNull(message = "Data de início é obrigatória")
        LocalDate dataInicio,

        @NotNull(message = "Data de fim é obrigatória")
        LocalDate dataFim
) {}

