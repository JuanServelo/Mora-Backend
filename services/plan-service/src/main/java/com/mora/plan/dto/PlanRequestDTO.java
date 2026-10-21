package com.mora.plan.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanRequestDTO {

    @NotBlank(message = "Este campo é obrigatório.")
    @Size(max = 100, message = "O nome não pode ultrapassar 100 caracteres.")
    @Schema(description = "Nome do plano.", example = "Completo")
    private String name;

    @NotNull(message = "Este campo é obrigatório.")
    @Min(value = 1, message = "Informe valores maiores que zero.")
    @Schema(description = "Quantos condomínios o cliente pode ter neste plano.", example = "10")
    private Integer maxCondominiums;

    @NotNull(message = "Este campo é obrigatório.")
    @Min(value = 1, message = "Informe valores maiores que zero.")
    @Schema(description = "Limite de usuários por condomínio.", example = "200")
    private Integer maxUsersPerCondominium;

    @NotNull(message = "Este campo é obrigatório.")
    @DecimalMin(value = "0.0", message = "O preço não pode ser negativo.")
    @Schema(description = "Mensalidade em reais.", example = "1000.00")
    private BigDecimal monthlyPrice;

    @NotEmpty(message = "Selecione um módulo.")
    @Schema(description = "Slugs dos módulos incluídos: portaria, reunioes, vagas, entregas, areas_comuns, reclamacoes, conhecimento, votacoes, veiculos, chaves, comunicacao, financeiro.", example = "[\"portaria\", \"reunioes\", \"financeiro\"]")
    private List<String> activeModules;
}
