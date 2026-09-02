package com.mora.plan.dto;

import com.mora.plan.enums.StatusAssinatura;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AssinaturaRequestDTO {

    @NotBlank(message = "condominioId é obrigatório")
    private String condominioId;

    @NotNull(message = "planId é obrigatório")
    private Long planId;

    private LocalDate vigenciaInicio;
    private LocalDate vigenciaFim;
    private StatusAssinatura status;
    private String observacao;
}
