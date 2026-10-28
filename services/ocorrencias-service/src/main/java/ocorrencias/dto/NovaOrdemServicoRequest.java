package ocorrencias.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ocorrencias.model.enums.PrioridadeOrdemServico;

import java.time.LocalDate;

/**
 * `reclamacaoId` nulo = manutenção preventiva. `responsavel` pode ficar vazio
 * quando há fornecedor: aí o responsável é ele.
 */
public record NovaOrdemServicoRequest(
        @NotBlank(message = "Descreva o serviço.")
        @Size(max = 5000, message = "A descrição pode ter até 5000 caracteres.")
        @Schema(example = "Trocar o registro do 3º andar.")
        String descricao,

        @NotNull(message = "Informe a prioridade.")
        PrioridadeOrdemServico prioridade,

        @NotNull(message = "Informe o prazo.")
        @Schema(example = "2026-11-05")
        LocalDate prazo,

        @Size(max = 150, message = "O responsável pode ter até 150 caracteres.")
        @Schema(example = "Hidráulica Silva")
        String responsavel,

        @Schema(example = "1")
        Long fornecedorId,

        @Schema(example = "3")
        Long reclamacaoId
) {}
