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

@RestController
@RequestMapping("/api/assinaturas")
@RequiredArgsConstructor
public class AssinaturaController {

    private final AssinaturaService assinaturaService;

    @GetMapping
    public List<AssinaturaResponseDTO> listar() {
        return assinaturaService.listarTodas();
    }

    /** Assinatura vigente de um condomínio — é o que o gestao-geral consome. */
    @GetMapping("/condominio/{condominioId}")
    public AssinaturaResponseDTO vigente(@PathVariable String condominioId) {
        return assinaturaService.buscarVigentePorCondominio(condominioId);
    }

    @GetMapping("/condominio/{condominioId}/historico")
    public List<AssinaturaResponseDTO> historico(@PathVariable String condominioId) {
        return assinaturaService.historicoDoCondominio(condominioId);
    }

    @PostMapping
    public ResponseEntity<AssinaturaResponseDTO> criar(@Valid @RequestBody AssinaturaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assinaturaService.criar(dto));
    }

    @PatchMapping("/{id}/status")
    public AssinaturaResponseDTO alterarStatus(@PathVariable Long id,
                                               @RequestParam StatusAssinatura status) {
        return assinaturaService.alterarStatus(id, status);
    }
}
