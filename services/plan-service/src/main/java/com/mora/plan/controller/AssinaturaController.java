package com.mora.plan.controller;

import com.mora.plan.dto.AssinaturaRequestDTO;
import com.mora.plan.dto.AssinaturaResponseDTO;
import com.mora.plan.enums.StatusAssinatura;
import com.mora.plan.service.AssinaturaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/assinaturas")
@RequiredArgsConstructor
@Tag(name = "Assinaturas", description = "Assinatura de cada condomínio, com vigência e status. É o modelo que o front e o gestao-geral leem. Exige token; o serviço ainda não restringe por perfil.")
public class AssinaturaController {

    private final AssinaturaService assinaturaService;

    @GetMapping
    @Operation(summary = "Listar assinaturas", description = "Todas as assinaturas, com o plano e se estão vigentes hoje. Usado pelo painel de receita do Admin Geral.")
    @ApiResponse(responseCode = "200", description = "Assinaturas.")
    public List<AssinaturaResponseDTO> listar() {
        return assinaturaService.listarTodas();
    }

    /** Assinatura vigente de um condomínio — é o que o gestao-geral consome. */
    @GetMapping("/condominio/{condominioId}")
    @Operation(summary = "Assinatura vigente do condomínio", description = "A assinatura ATIVA dentro da vigência; 404 se não houver. É o que o gestao-geral consome para o painel.")
    @Parameter(name = "condominioId", description = "Id do condomínio.", example = "cond-parque-verde")
    @ApiResponse(responseCode = "200", description = "Assinatura vigente.")
    public AssinaturaResponseDTO vigente(@PathVariable String condominioId) {
        return assinaturaService.buscarVigentePorCondominio(condominioId);
    }

    @GetMapping("/condominio/{condominioId}/historico")
    @Operation(summary = "Histórico do condomínio", description = "Todas as assinaturas que o condomínio já teve, da mais recente para a mais antiga.")
    @Parameter(name = "condominioId", description = "Id do condomínio.", example = "cond-parque-verde")
    @ApiResponse(responseCode = "200", description = "Assinaturas.")
    public List<AssinaturaResponseDTO> historico(@PathVariable String condominioId) {
        return assinaturaService.historicoDoCondominio(condominioId);
    }

    @PostMapping
    @Operation(summary = "Criar assinatura", description = "Vincula o condomínio a um plano, com vigência. Sem status, nasce ATIVA.")
    @ApiResponse(responseCode = "201", description = "Assinatura criada.")
    public ResponseEntity<AssinaturaResponseDTO> criar(@Valid @RequestBody AssinaturaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assinaturaService.criar(dto));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Mudar status", description = "ATIVA, SUSPENSA ou CANCELADA. Suspensa ou cancelada deixa de ser vigente na hora.")
    @Parameter(name = "id", description = "Id da assinatura.", example = "7")
    @ApiResponse(responseCode = "200", description = "Assinatura atualizada.")
    public AssinaturaResponseDTO alterarStatus(@PathVariable Long id,
                                               @Parameter(description = "Novo status.", example = "SUSPENSA") @RequestParam StatusAssinatura status) {
        return assinaturaService.alterarStatus(id, status);
    }
}
