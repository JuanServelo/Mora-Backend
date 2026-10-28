package ocorrencias.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AndamentoRequest(
        @NotBlank(message = "Descreva o andamento.")
        @Size(max = 5000, message = "O andamento pode ter até 5000 caracteres.")
        @Schema(example = "Peça comprada; instalação amanhã.")
        String mensagem
) {}
