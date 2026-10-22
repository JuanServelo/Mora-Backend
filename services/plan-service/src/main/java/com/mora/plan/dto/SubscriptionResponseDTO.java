package com.mora.plan.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionResponseDTO {

    @Schema(description = "Id da assinatura.", example = "7")
    private Long id;
    @Schema(description = "Id do condomínio (auth-api).", example = "cond-parque-verde")
    private String condominioId;
    @Schema(description = "Id do plano.", example = "1")
    private Long planId;
    @Schema(description = "Nome do plano.", example = "Completo")
    private String planName;
    @Schema(description = "ACTIVE, SUSPENDED ou CANCELLED.", example = "ACTIVE")
    private String status;
    @Schema(description = "Início da assinatura.", example = "2026-01-01")
    private LocalDate startDate;
    @Schema(description = "Fim da assinatura; vazio para sem prazo.", example = "2026-12-31")
    private LocalDate endDate;
    @Schema(description = "Quem criou (e-mail do token).", example = "admin@mora.test")
    private String createdBy;
    @Schema(description = "Quem alterou por último.", example = "admin@mora.test")
    private String updatedBy;
    @Schema(description = "Quando foi criada.", example = "2026-01-01T09:00:00")
    private LocalDateTime createdAt;
    @Schema(description = "Última alteração.", example = "2026-01-01T09:00:00")
    private LocalDateTime updatedAt;
}
