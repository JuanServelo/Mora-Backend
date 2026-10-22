package com.mora.plan.dto;

import com.mora.plan.enums.StatusAssinatura;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class AssinaturaRequestDTO {

    @NotBlank(message = "condominioId é obrigatório")
    @Schema(description = "Id do condomínio (auth-api).", example = "cond-parque-verde")
    private String condominioId;

    @NotNull(message = "planId é obrigatório")
    @Schema(description = "Id do plano.", example = "1")
    private Long planId;

    @Schema(description = "Início da vigência.", example = "2026-01-01")
    private LocalDate vigenciaInicio;
    @Schema(description = "Fim da vigência; vazio para sem prazo.", example = "2026-12-31")
    private LocalDate vigenciaFim;
    @Schema(description = "ATIVA, SUSPENSA ou CANCELADA (padrão ATIVA).", example = "ATIVA")
    private StatusAssinatura status;
    @Schema(description = "Anotação livre.", example = "Contrato anual assinado em dezembro.")
    private String observacao;
}
