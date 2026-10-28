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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/api/plans/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Assinaturas (subscriptions)", description = "API de compatibilidade em inglês para tb_assinaturas. Escritas exigem ADMIN_GERAL; leitura restrita ao próprio condomínio.")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    @Operation(summary = "Criar assinatura", description = "Vincula um condomínio a um plano ativo. Um condomínio tem no máximo uma assinatura ACTIVE.")
    @ApiResponse(responseCode = "201", description = "Assinatura criada.")
    public ResponseEntity<SubscriptionResponseDTO> create(@Valid @RequestBody SubscriptionRequestDTO dto) {
        SubscriptionResponseDTO created = subscriptionService.createSubscription(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "Listar assinaturas", description = "Todas as assinaturas, de todos os condomínios.")
    @ApiResponse(responseCode = "200", description = "Assinaturas.")
    public ResponseEntity<List<SubscriptionResponseDTO>> listAll() {
        return ResponseEntity.ok(subscriptionService.listAll());
    }

    @GetMapping("/condominio/{condominioId}")
    @Operation(summary = "Assinatura ativa do condomínio", description = "A assinatura ACTIVE do condomínio; 404 se não houver.")
    @Parameter(name = "condominioId", description = "Id do condomínio.", example = "cond-parque-verde")
    @ApiResponse(responseCode = "200", description = "Assinatura.")
    public ResponseEntity<SubscriptionResponseDTO> getByCondominioId(@PathVariable String condominioId) {
        return ResponseEntity.ok(subscriptionService.getByCondominioId(condominioId));
    }

    @PatchMapping("/{id}/suspend")
    @Operation(summary = "Suspender assinatura", description = "Passa uma assinatura ACTIVE a SUSPENDED.")
    @Parameter(name = "id", description = "Id da assinatura.", example = "7")
    @ApiResponse(responseCode = "200", description = "Assinatura suspensa.")
    public ResponseEntity<SubscriptionResponseDTO> suspend(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.suspend(id));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancelar assinatura", description = "Passa a assinatura a CANCELLED.")
    @Parameter(name = "id", description = "Id da assinatura.", example = "7")
    @ApiResponse(responseCode = "200", description = "Assinatura cancelada.")
    public ResponseEntity<SubscriptionResponseDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.cancel(id));
    }

    @PutMapping("/{condominioId}/change-plan")
    @Operation(summary = "Trocar de plano", description = "Cancela a assinatura atual do condomínio e cria outra, com o plano novo, a partir de hoje.")
    @Parameter(name = "condominioId", description = "Id do condomínio.", example = "cond-parque-verde")
    @ApiResponse(responseCode = "200", description = "Nova assinatura.")
    public ResponseEntity<SubscriptionResponseDTO> changePlan(
            @PathVariable String condominioId,
            @Valid @RequestBody ChangePlanRequestDTO dto) {
        return ResponseEntity.ok(subscriptionService.changePlan(condominioId, dto));
    }

    @GetMapping("/condominio/{condominioId}/modules")
    @Operation(summary = "Módulos do condomínio", description = "Slugs dos módulos da assinatura ativa: é o que o front consulta para mostrar ou esconder telas.")
    @Parameter(name = "condominioId", description = "Id do condomínio.", example = "cond-parque-verde")
    @ApiResponse(responseCode = "200", description = "Slugs dos módulos.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            ["portaria", "reunioes", "financeiro"]""")))
    public ResponseEntity<List<String>> getModules(@PathVariable String condominioId) {
        return ResponseEntity.ok(subscriptionService.getModulesByCondominioId(condominioId));
    }
}
