package com.mora.plan.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanRequestDTO {

    @NotBlank(message = "Este campo é obrigatório.")
    @Size(max = 100, message = "O nome não pode ultrapassar 100 caracteres.")
    private String name;

    @NotNull(message = "Este campo é obrigatório.")
    @Min(value = 1, message = "Informe valores maiores que zero.")
    private Integer maxCondominiums;

    @NotNull(message = "Este campo é obrigatório.")
    @Min(value = 1, message = "Informe valores maiores que zero.")
    private Integer maxUsersPerCondominium;

    @NotNull(message = "Este campo é obrigatório.")
    @DecimalMin(value = "0.0", message = "O preço não pode ser negativo.")
    private BigDecimal monthlyPrice;

    @NotEmpty(message = "Selecione um módulo.")
    private List<String> activeModules;
}
