package com.mora.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionRequestDTO {

    @NotBlank(message = "O ID do condomínio é obrigatório")
    private String condominioId;

    @NotNull(message = "O ID do plano é obrigatório")
    private Long planId;
}
