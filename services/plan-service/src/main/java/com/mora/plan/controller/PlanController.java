package com.mora.plan.controller;

import com.mora.plan.dto.PlanRequestDTO;
import com.mora.plan.dto.PlanResponseDTO;
import com.mora.plan.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
@Tag(name = "Planos SaaS", description = "Endpoints para gerenciamento de planos SaaS da plataforma")
public class PlanController {

    private final PlanService planService;

    @PostMapping
    @Operation(summary = "Cadastrar plano", description = "Cadastra um novo plano SaaS com nome, limites, preço e módulos ativos.")
    public ResponseEntity<PlanResponseDTO> createPlan(@Valid @RequestBody PlanRequestDTO dto) {
        PlanResponseDTO created = planService.createPlan(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "Listar planos", description = "Lista todos os planos cadastrados na plataforma.")
    public ResponseEntity<List<PlanResponseDTO>> listPlans() {
        List<PlanResponseDTO> plans = planService.listPlans();
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar plano", description = "Busca um plano pelo ID.")
    public ResponseEntity<PlanResponseDTO> getPlan(@PathVariable Long id) {
        PlanResponseDTO plan = planService.getPlan(id);
        return ResponseEntity.ok(plan);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar plano", description = "Edita um plano existente atualizando nome, limites, preço e módulos.")
    public ResponseEntity<PlanResponseDTO> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody PlanRequestDTO dto) {
        PlanResponseDTO updated = planService.updatePlan(id, dto);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Ativar/desativar plano", description = "Alterna o status de ativação de um plano.")
    public ResponseEntity<PlanResponseDTO> togglePlanStatus(@PathVariable Long id) {
        PlanResponseDTO toggled = planService.togglePlanStatus(id);
        return ResponseEntity.ok(toggled);
    }

    @PatchMapping("/{id}/modules")
    @Operation(summary = "Atualizar módulos", description = "Atualiza a lista de módulos ativos de um plano.")
    public ResponseEntity<PlanResponseDTO> updateModules(
            @PathVariable Long id,
            @RequestBody List<String> modules) {
        PlanResponseDTO updated = planService.updateModules(id, modules);
        return ResponseEntity.ok(updated);
    }
}
