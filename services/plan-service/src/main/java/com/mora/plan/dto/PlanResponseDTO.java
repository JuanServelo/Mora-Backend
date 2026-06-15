package com.mora.plan.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlanResponseDTO {

    private Long id;
    private String name;
    private Integer maxCondominiums;
    private Integer maxUsersPerCondominium;
    private BigDecimal monthlyPrice;
    private List<String> activeModules;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
