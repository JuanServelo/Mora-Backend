package com.mora.plan.mapper;

import com.mora.plan.dto.SubscriptionResponseDTO;
import com.mora.plan.entity.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SubscriptionMapper {

    @Mapping(source = "plan.id", target = "planId")
    @Mapping(source = "plan.name", target = "planName")
    @Mapping(source = "status", target = "status")
    SubscriptionResponseDTO toResponseDto(Subscription subscription);
}
