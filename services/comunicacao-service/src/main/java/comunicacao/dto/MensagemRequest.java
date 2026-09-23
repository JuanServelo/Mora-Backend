package comunicacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de POST /chat/mensagem. */
public record MensagemRequest(
        @NotBlank(message = "Destinatário é obrigatório")
        String destinatarioId,

        @NotBlank(message = "Mensagem não pode ser vazia")
        @Size(max = 4000, message = "Mensagem muito longa (máximo 4000 caracteres)")
        String texto
) {}
