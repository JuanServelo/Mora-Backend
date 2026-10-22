package com.mora.plan.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlanResponseDTO {

    @Schema(description = "Id do plano.", example = "1")
    private Long id;
    @Schema(description = "Nome do plano.", example = "Completo")
    private String name;
    @Schema(description = "Quantos condomínios o cliente pode ter neste plano.", example = "10")
    private Integer maxCondominiums;
    @Schema(description = "Limite de usuários por condomínio.", example = "200")
    private Integer maxUsersPerCondominium;
    @Schema(description = "Mensalidade em reais.", example = "1000.00")
    private BigDecimal monthlyPrice;
    @Schema(description = "Slugs dos módulos incluídos: portaria, reunioes, vagas, entregas, areas_comuns, reclamacoes, conhecimento, votacoes, veiculos, chaves, comunicacao, financeiro.", example = "[\"portaria\", \"reunioes\", \"financeiro\"]")
    private List<String> activeModules;
    @Schema(description = "Plano disponível para novas assinaturas.", example = "true")
    private Boolean isActive;
    @Schema(description = "Quando foi criado.", example = "2026-01-05T10:00:00")
    private LocalDateTime createdAt;
    @Schema(description = "Última alteração.", example = "2026-09-01T16:30:00")
    private LocalDateTime updatedAt;
}
