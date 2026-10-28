package ocorrencias.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FornecedorRequest(
        @NotBlank(message = "Informe o nome do fornecedor.")
        @Size(max = 150, message = "O nome pode ter até 150 caracteres.")
        @Schema(example = "Hidráulica Silva")
        String nome,

        @Size(max = 100, message = "O serviço pode ter até 100 caracteres.")
        @Schema(example = "Hidráulica")
        String servico,

        @Size(max = 20, message = "O telefone pode ter até 20 caracteres.")
        @Schema(example = "(41) 99999-0000")
        String telefone,

        @Email(message = "E-mail inválido.")
        @Size(max = 150, message = "O e-mail pode ter até 150 caracteres.")
        @Schema(example = "contato@hidraulicasilva.com")
        String email,

        @Size(max = 20, message = "O documento pode ter até 20 caracteres.")
        @Schema(example = "12.345.678/0001-90")
        String documento,

        /** Nulo no cadastro (nasce ativo); na edição, desativa ou reativa. */
        Boolean ativo
) {}
