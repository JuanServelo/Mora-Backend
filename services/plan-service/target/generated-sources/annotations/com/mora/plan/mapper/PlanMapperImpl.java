package com.mora.plan.mapper;

import com.mora.plan.dto.PlanRequestDTO;
import com.mora.plan.dto.PlanResponseDTO;
import com.mora.plan.entity.Plan;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-15T02:40:25-0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class PlanMapperImpl implements PlanMapper {

    @Override
    public Plan toEntity(PlanRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Plan.PlanBuilder plan = Plan.builder();

        plan.name( dto.getName() );
        plan.maxCondominiums( dto.getMaxCondominiums() );
        plan.maxUsersPerCondominium( dto.getMaxUsersPerCondominium() );
        plan.monthlyPrice( dto.getMonthlyPrice() );
        List<String> list = dto.getActiveModules();
        if ( list != null ) {
            plan.activeModules( new ArrayList<String>( list ) );
        }

        return plan.build();
    }

    @Override
    public PlanResponseDTO toResponseDto(Plan plan) {
        if ( plan == null ) {
            return null;
        }

        PlanResponseDTO.PlanResponseDTOBuilder planResponseDTO = PlanResponseDTO.builder();

        planResponseDTO.id( plan.getId() );
        planResponseDTO.name( plan.getName() );
        planResponseDTO.maxCondominiums( plan.getMaxCondominiums() );
        planResponseDTO.maxUsersPerCondominium( plan.getMaxUsersPerCondominium() );
        planResponseDTO.monthlyPrice( plan.getMonthlyPrice() );
        List<String> list = plan.getActiveModules();
        if ( list != null ) {
            planResponseDTO.activeModules( new ArrayList<String>( list ) );
        }
        planResponseDTO.isActive( plan.getIsActive() );
        planResponseDTO.createdAt( plan.getCreatedAt() );
        planResponseDTO.updatedAt( plan.getUpdatedAt() );

        return planResponseDTO.build();
    }
}
