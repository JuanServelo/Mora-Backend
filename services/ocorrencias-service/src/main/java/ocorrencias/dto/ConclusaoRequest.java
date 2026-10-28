package ocorrencias.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ConclusaoRequest(
        @NotNull(message = "Informe o custo (0 se não houve).")
        @PositiveOrZero(message = "O custo não pode ser negativo.")
        @Schema(example = "250.50")
        BigDecimal custo,

        @NotBlank(message = "Registre as observações do encerramento.")
        @Size(max = 5000, message = "As observações podem ter até 5000 caracteres.")
        @Schema(example = "Registro trocado e testado.")
        String observacoes
) {}
