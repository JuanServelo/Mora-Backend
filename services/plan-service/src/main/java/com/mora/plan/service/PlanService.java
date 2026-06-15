package com.mora.plan.service;

import com.mora.plan.dto.PlanRequestDTO;
import com.mora.plan.dto.PlanResponseDTO;
import com.mora.plan.entity.Plan;
import com.mora.plan.enums.PlanModule;
import com.mora.plan.mapper.PlanMapper;
import com.mora.plan.repository.PlanRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final PlanMapper planMapper;

    /**
     * US-01.1 — Cadastro de plano
     */
    @Transactional
    public PlanResponseDTO createPlan(@NotNull PlanRequestDTO dto) {
        // CA-02: Nome duplicado
        if (planRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("Já existe um plano com este nome.");
        }

        // CA-04.3: Validar módulos
        validateModules(dto.getActiveModules());

        Plan plan = planMapper.toEntity(dto);
        plan.setIsActive(true);

        Plan saved = planRepository.save(plan);
        return planMapper.toResponseDto(saved);
    }

    /**
     * US-01.5 — Consulta de planos
     */
    @Transactional(readOnly = true)
    public List<PlanResponseDTO> listPlans() {
        return planRepository.findAll().stream()
                .map(planMapper::toResponseDto)
                .toList();
    }

    /**
     * Busca plano por ID
     */
    @Transactional(readOnly = true)
    public PlanResponseDTO getPlan(@NotNull Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plano não encontrado com o ID: " + id));
        return planMapper.toResponseDto(plan);
    }

    /**
     * US-01.2 — Edição de plano
     */
    @Transactional
    public PlanResponseDTO updatePlan(@NotNull Long id, @NotNull PlanRequestDTO dto) {
        Plan existing = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plano não encontrado com o ID: " + id));

        // CA-02: Nome duplicado (diferente do próprio plano)
        Optional<Plan> byName = planRepository.findByNameIgnoreCase(dto.getName());
        if (byName.isPresent() && !byName.get().getId().equals(id)) {
            throw new IllegalArgumentException("Já existe um plano com este nome.");
        }

        // CA-04.3: Validar módulos
        validateModules(dto.getActiveModules());

        // CA-03: Limite inconsistente com tenants ativos
        // TODO: Quando o serviço de tenants existir, validar aqui se a redução de limites
        // não conflita com tenants já contratados. Placeholder para integração futura.
        // Exemplo:
        // if (dto.getMaxCondominiums() < tenantService.getMaxUsageForPlan(id)) {
        //     throw new IllegalStateException("Existem tenants que não atendem a este novo limite.");
        // }

        existing.setName(dto.getName());
        existing.setMaxCondominiums(dto.getMaxCondominiums());
        existing.setMaxUsersPerCondominium(dto.getMaxUsersPerCondominium());
        existing.setMonthlyPrice(dto.getMonthlyPrice());
        existing.getActiveModules().clear();
        existing.getActiveModules().addAll(dto.getActiveModules());

        Plan updated = planRepository.save(existing);
        return planMapper.toResponseDto(updated);
    }

    /**
     * US-01.3 — Ativação e desativação de plano
     */
    @Transactional
    public PlanResponseDTO togglePlanStatus(@NotNull Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plano não encontrado com o ID: " + id));

        plan.setIsActive(!plan.getIsActive());
        Plan saved = planRepository.save(plan);

        return planMapper.toResponseDto(saved);
    }

    /**
     * US-01.4 — Configuração de módulos do plano
     */
    @Transactional
    public PlanResponseDTO updateModules(@NotNull Long id, @NotNull List<String> modules) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plano não encontrado com o ID: " + id));

        if (modules == null || modules.isEmpty()) {
            throw new IllegalArgumentException("Selecione um módulo.");
        }

        validateModules(modules);

        plan.getActiveModules().clear();
        plan.getActiveModules().addAll(modules);

        Plan saved = planRepository.save(plan);
        return planMapper.toResponseDto(saved);
    }

    /**
     * Valida que todos os slugs de módulo são válidos.
     */
    private void validateModules(List<String> modules) {
        if (modules == null || modules.isEmpty()) {
            throw new IllegalArgumentException("Selecione um módulo.");
        }
        for (String slug : modules) {
            if (!PlanModule.isValid(slug)) {
                throw new IllegalArgumentException("Módulo inválido: " + slug);
            }
        }
    }
}
