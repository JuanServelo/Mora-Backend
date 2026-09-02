package vagas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import vagas.model.enums.ModalidadeAluguel;
import java.time.LocalDate;

/**
 * DTO para solicitar o aluguel de uma vaga disponível (RF10 US2).
 */
public record AluguelRequestDTO(

        @NotBlank(message = "ID da vaga é obrigatório")
        String vagaId,

        @NotNull(message = "Data de início é obrigatória")
        LocalDate dataInicio,

        @NotNull(message = "Data de fim é obrigatória")
        LocalDate dataFim,

        @NotNull(message = "Modalidade é obrigatória (DIARIA ou MENSAL)")
        ModalidadeAluguel modalidade
) {}

