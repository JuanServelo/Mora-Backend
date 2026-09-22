package com.mora.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionRequestDTO {

    @NotBlank(message = "O ID do condomínio é obrigatório.")
    private String condominioId;

    @NotNull(message = "O ID do plano é obrigatório.")
    private Long planId;

    @NotNull(message = "A data de início é obrigatória.")
    private LocalDate startDate;

    private LocalDate endDate;
}
