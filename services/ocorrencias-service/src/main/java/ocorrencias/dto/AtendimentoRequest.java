package ocorrencias.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ocorrencias.model.enums.StatusReclamacao;

/**
 * A resposta do síndico. `status` é opcional: sem ele é só resposta; com ele,
 * a `mensagem` é também a justificativa da mudança.
 */
public record AtendimentoRequest(
        StatusReclamacao status,

        @NotBlank(message = "Escreva a resposta ou a justificativa.")
        @Size(max = 5000, message = "A resposta pode ter até 5000 caracteres.")
        @Schema(example = "Zelador vai trocar a lâmpada amanhã de manhã.")
        String mensagem
) {}
