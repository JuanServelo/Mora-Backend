package com.mora.plan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePlanRequestDTO {

    @NotNull(message = "O ID do novo plano é obrigatório.")
    @Schema(description = "Id do novo plano.", example = "2")
    private Long newPlanId;
}
