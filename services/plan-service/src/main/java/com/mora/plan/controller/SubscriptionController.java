package com.mora.plan.controller;

import com.mora.plan.dto.ChangePlanRequestDTO;
import com.mora.plan.dto.SubscriptionRequestDTO;
import com.mora.plan.dto.SubscriptionResponseDTO;
import com.mora.plan.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Assinaturas", description = "Endpoints para gerenciamento de assinaturas de condomínios")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    @Operation(summary = "Criar assinatura", description = "Cria uma nova assinatura vinculando um condomínio a um plano.")
    public ResponseEntity<SubscriptionResponseDTO> create(@Valid @RequestBody SubscriptionRequestDTO dto) {
        SubscriptionResponseDTO created = subscriptionService.createSubscription(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "Listar assinaturas", description = "Lista todas as assinaturas cadastradas.")
    public ResponseEntity<List<SubscriptionResponseDTO>> listAll() {
        return ResponseEntity.ok(subscriptionService.listAll());
    }

    @GetMapping("/condominio/{condominioId}")
    @Operation(summary = "Buscar assinatura ativa", description = "Busca a assinatura ativa de um condomínio.")
    public ResponseEntity<SubscriptionResponseDTO> getByCondominioId(@PathVariable String condominioId) {
        return ResponseEntity.ok(subscriptionService.getByCondominioId(condominioId));
    }

    @PatchMapping("/{id}/suspend")
    @Operation(summary = "Suspender assinatura", description = "Suspende uma assinatura ativa.")
    public ResponseEntity<SubscriptionResponseDTO> suspend(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.suspend(id));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancelar assinatura", description = "Cancela uma assinatura.")
    public ResponseEntity<SubscriptionResponseDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.cancel(id));
    }

    @PutMapping("/{condominioId}/change-plan")
    @Operation(summary = "Trocar plano", description = "Cancela a assinatura atual do condomínio e cria uma nova com o plano informado.")
    public ResponseEntity<SubscriptionResponseDTO> changePlan(
            @PathVariable String condominioId,
            @Valid @RequestBody ChangePlanRequestDTO dto) {
        return ResponseEntity.ok(subscriptionService.changePlan(condominioId, dto));
    }

    @GetMapping("/condominio/{condominioId}/modules")
    @Operation(summary = "Módulos ativos", description = "Retorna a lista de slugs dos módulos ativos do condomínio.")
    public ResponseEntity<List<String>> getModules(@PathVariable String condominioId) {
        return ResponseEntity.ok(subscriptionService.getModulesByCondominioId(condominioId));
    }
}
