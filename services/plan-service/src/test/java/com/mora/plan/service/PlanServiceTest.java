package com.mora.plan.service;

import com.mora.plan.dto.PlanRequestDTO;
import com.mora.plan.dto.PlanResponseDTO;
import com.mora.plan.entity.Plan;
import com.mora.plan.mapper.PlanMapper;
import com.mora.plan.repository.PlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private PlanMapper planMapper;

    @InjectMocks
    private PlanService planService;

    private PlanRequestDTO planRequestDTO;
    private Plan plan;
    private PlanResponseDTO planResponseDTO;

    @BeforeEach
    void setUp() {
        planRequestDTO = PlanRequestDTO.builder()
                .name("Plano Teste")
                .maxCondominiums(5)
                .maxUsersPerCondominium(100)
                .monthlyPrice(new BigDecimal("99.90"))
                .activeModules(List.of("MEETINGS"))
                .build();

        plan = Plan.builder()
                .id(1L)
                .name("Plano Teste")
                .maxCondominiums(5)
                .maxUsersPerCondominium(100)
                .monthlyPrice(new BigDecimal("99.90"))
                .activeModules(new ArrayList<>(List.of("MEETINGS")))
                .isActive(true)
                .build();

        planResponseDTO = PlanResponseDTO.builder()
                .id(1L)
                .name("Plano Teste")
                .maxCondominiums(5)
                .maxUsersPerCondominium(100)
                .monthlyPrice(new BigDecimal("99.90"))
                .activeModules(List.of("MEETINGS"))
                .isActive(true)
                .build();
    }

    @Test
    void createPlan_Success() {
        when(planRepository.existsByNameIgnoreCase("Plano Teste")).thenReturn(false);
        when(planMapper.toEntity(any(PlanRequestDTO.class))).thenReturn(plan);
        when(planRepository.save(any(Plan.class))).thenReturn(plan);
        when(planMapper.toResponseDto(any(Plan.class))).thenReturn(planResponseDTO);

        PlanResponseDTO result = planService.createPlan(planRequestDTO);

        assertNotNull(result);
        assertEquals("Plano Teste", result.getName());
        verify(planRepository).save(any(Plan.class));
    }

    @Test
    void createPlan_ThrowsExceptionWhenNameExists() {
        when(planRepository.existsByNameIgnoreCase("Plano Teste")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            planService.createPlan(planRequestDTO);
        });

        assertEquals("Já existe um plano com este nome.", exception.getMessage());
        verify(planRepository, never()).save(any(Plan.class));
    }

    @Test
    void getPlan_Success() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(planMapper.toResponseDto(any(Plan.class))).thenReturn(planResponseDTO);

        PlanResponseDTO result = planService.getPlan(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void updatePlan_Success() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(planRepository.findByNameIgnoreCase("Plano Teste")).thenReturn(Optional.of(plan));
        when(planRepository.save(any(Plan.class))).thenReturn(plan);
        when(planMapper.toResponseDto(any(Plan.class))).thenReturn(planResponseDTO);

        PlanResponseDTO result = planService.updatePlan(1L, planRequestDTO);

        assertNotNull(result);
        assertEquals("Plano Teste", result.getName());
        verify(planRepository).save(any(Plan.class));
    }

    @Test
    void togglePlanStatus_Success() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(planRepository.save(any(Plan.class))).thenReturn(plan);
        when(planMapper.toResponseDto(any(Plan.class))).thenReturn(planResponseDTO);

        planService.togglePlanStatus(1L);

        assertFalse(plan.getIsActive()); // it was true, now it's false
        verify(planRepository).save(plan);
    }
}
