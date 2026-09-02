package com.mora.plan.controller;

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
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Assinaturas SaaS", description = "Endpoints para gerenciamento de assinaturas de condomínios")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    @Operation(summary = "Criar assinatura", description = "Cria uma nova assinatura para um condomínio e cancela a anterior, se houver.")
    public ResponseEntity<SubscriptionResponseDTO> createSubscription(@Valid @RequestBody SubscriptionRequestDTO dto) {
        SubscriptionResponseDTO created = subscriptionService.createSubscription(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/condominium/{condominioId}")
    @Operation(summary = "Listar assinaturas do condomínio", description = "Lista todas as assinaturas vinculadas a um condomínio.")
    public ResponseEntity<List<SubscriptionResponseDTO>> getSubscriptionsByCondominium(@PathVariable String condominioId) {
        List<SubscriptionResponseDTO> subscriptions = subscriptionService.getSubscriptionsByCondominium(condominioId);
        return ResponseEntity.ok(subscriptions);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancelar assinatura", description = "Cancela uma assinatura ativa.")
    public ResponseEntity<SubscriptionResponseDTO> cancelSubscription(@PathVariable Long id) {
        SubscriptionResponseDTO canceled = subscriptionService.cancelSubscription(id);
        return ResponseEntity.ok(canceled);
    }
}
