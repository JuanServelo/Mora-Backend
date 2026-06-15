package com.mora.plan.mapper;

import com.mora.plan.dto.PlanRequestDTO;
import com.mora.plan.dto.PlanResponseDTO;
import com.mora.plan.entity.Plan;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PlanMapper {

    Plan toEntity(PlanRequestDTO dto);

    PlanResponseDTO toResponseDto(Plan plan);
}
