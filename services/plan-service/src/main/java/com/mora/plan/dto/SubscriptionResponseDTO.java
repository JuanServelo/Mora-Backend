package com.mora.plan.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionResponseDTO {

    private Long id;
    private String condominioId;
    private Long planId;
    private String planName;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
