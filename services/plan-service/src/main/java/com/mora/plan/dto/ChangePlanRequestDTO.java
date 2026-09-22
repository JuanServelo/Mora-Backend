package com.mora.plan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePlanRequestDTO {

    @NotNull(message = "O ID do novo plano é obrigatório.")
    private Long newPlanId;
}
