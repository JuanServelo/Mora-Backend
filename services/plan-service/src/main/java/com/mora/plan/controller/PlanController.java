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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
@Tag(name = "Planos", description = "Planos comerciais da plataforma: limites, mensalidade e módulos incluídos. Exige token; o serviço ainda não restringe por perfil.")
public class PlanController {

    private final PlanService planService;

    @PostMapping
    @Operation(summary = "Cadastrar plano", description = "Cria um plano com nome, limites, mensalidade e módulos. Módulo fora da lista conhecida é recusado.")
    @ApiResponse(responseCode = "201", description = "Plano criado.")
    public ResponseEntity<PlanResponseDTO> createPlan(@Valid @RequestBody PlanRequestDTO dto) {
        PlanResponseDTO created = planService.createPlan(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "Listar planos", description = "Todos os planos, ativos e inativos.")
    @ApiResponse(responseCode = "200", description = "Planos.")
    public ResponseEntity<List<PlanResponseDTO>> listPlans() {
        List<PlanResponseDTO> plans = planService.listPlans();
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do plano", description = "Um plano pelo id.")
    @Parameter(name = "id", description = "Id do plano.", example = "1")
    @ApiResponse(responseCode = "200", description = "Plano.")
    public ResponseEntity<PlanResponseDTO> getPlan(@PathVariable Long id) {
        PlanResponseDTO plan = planService.getPlan(id);
        return ResponseEntity.ok(plan);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar plano", description = "Substitui nome, limites, mensalidade e módulos. Assinaturas existentes passam a ver os novos valores.")
    @Parameter(name = "id", description = "Id do plano.", example = "1")
    @ApiResponse(responseCode = "200", description = "Plano atualizado.")
    public ResponseEntity<PlanResponseDTO> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody PlanRequestDTO dto) {
        PlanResponseDTO updated = planService.updatePlan(id, dto);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Ativar ou desativar plano", description = "Inverte `isActive`. Plano inativo não aceita assinatura nova.")
    @Parameter(name = "id", description = "Id do plano.", example = "1")
    @ApiResponse(responseCode = "200", description = "Plano com o status invertido.")
    public ResponseEntity<PlanResponseDTO> togglePlanStatus(@PathVariable Long id) {
        PlanResponseDTO toggled = planService.togglePlanStatus(id);
        return ResponseEntity.ok(toggled);
    }

    @PatchMapping("/{id}/modules")
    @Operation(summary = "Trocar módulos do plano", description = "Substitui a lista de módulos incluídos, pelos slugs.")
    @Parameter(name = "id", description = "Id do plano.", example = "1")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Slugs dos módulos.", required = true,
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            ["portaria", "reunioes", "financeiro", "comunicacao"]""")))
    @ApiResponse(responseCode = "200", description = "Plano atualizado.")
    public ResponseEntity<PlanResponseDTO> updateModules(
            @PathVariable Long id,
            @RequestBody List<String> modules) {
        PlanResponseDTO updated = planService.updateModules(id, modules);
        return ResponseEntity.ok(updated);
    }
}
