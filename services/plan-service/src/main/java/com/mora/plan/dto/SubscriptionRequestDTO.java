package com.mora.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionRequestDTO {

    @NotBlank(message = "O ID do condomínio é obrigatório.")
    @Schema(description = "Id do condomínio (auth-api).", example = "cond-parque-verde")
    private String condominioId;

    @NotNull(message = "O ID do plano é obrigatório.")
    @Schema(description = "Id do plano.", example = "1")
    private Long planId;

    @NotNull(message = "A data de início é obrigatória.")
    @Schema(description = "Início da assinatura.", example = "2026-01-01")
    private LocalDate startDate;

    @Schema(description = "Fim da assinatura; vazio para sem prazo.", example = "2026-12-31")
    private LocalDate endDate;
}
