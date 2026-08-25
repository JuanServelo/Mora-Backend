package com.mora.vagas.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para recusar uma solicitação de aluguel com justificativa obrigatória (RF10 US3).
 */
public record RecusaRequestDTO(
        @NotBlank(message = "Justificativa da recusa é obrigatória")
        String justificativa
) {}


