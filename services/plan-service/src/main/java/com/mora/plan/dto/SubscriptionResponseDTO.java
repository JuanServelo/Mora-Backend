package com.mora.plan.dto;

import com.mora.plan.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionResponseDTO {
    private Long id;
    private String condominioId;
    private PlanResponseDTO plan;
    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
