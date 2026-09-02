package com.mora.plan.mapper;

import com.mora.plan.dto.SubscriptionResponseDTO;
import com.mora.plan.entity.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {PlanMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SubscriptionMapper {

    @Mapping(target = "plan", source = "plan")
    SubscriptionResponseDTO toResponseDto(Subscription subscription);
}
