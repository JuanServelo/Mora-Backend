package ocorrencias.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NovaReclamacaoRequest(
        @NotBlank(message = "Informe a categoria.")
        @Schema(example = "ILUMINACAO")
        String categoria,

        @NotBlank(message = "Descreva a ocorrência.")
        @Size(max = 5000, message = "A descrição pode ter até 5000 caracteres.")
        @Schema(example = "Lâmpada do hall do bloco A queimada desde ontem.")
        String descricao,

        @Size(max = 500, message = "O endereço do anexo pode ter até 500 caracteres.")
        @Schema(example = "https://exemplo.com/foto-hall.jpg")
        String anexoUrl
) {}
