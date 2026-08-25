package com.mora.vagas.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mora.vagas.dto.VagaRequestDTO;
import com.mora.vagas.dto.VagaResponseDTO;
import com.mora.vagas.exception.OperacaoInvalidaException;
import com.mora.vagas.entity.Vaga;
import com.mora.vagas.service.VagaService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/vagas")
@RequiredArgsConstructor
public class VagaController {

    private final VagaService vagaService;

    /**
     * Apenas ADMINISTRADOR pode cadastrar vagas (RF09).
     * O role é propagado pelo API Gateway (Traefik) no header X-User-Role.
     */
    @PostMapping("/cadastrar")
    public ResponseEntity<VagaResponseDTO> cadastrar(
            @Valid @RequestBody VagaRequestDTO dto,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        validarAdministrador(role);
        Vaga salvo = vagaService.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(VagaResponseDTO.fromEntity(salvo));
    }

    @GetMapping
    public List<VagaResponseDTO> listarApenasAtivas() {
        return vagaService.listarApenasAtivas().stream()
                .map(VagaResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/todas")
    public List<VagaResponseDTO> listarTodas(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        validarAdministrador(role);
        return vagaService.listarTodas().stream()
                .map(VagaResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public VagaResponseDTO buscarPorId(@PathVariable String id) {
        return VagaResponseDTO.fromEntity(vagaService.buscarPorId(id));
    }

    @GetMapping("/apartamento/{apartamentoId}")
    public List<VagaResponseDTO> buscarPorApartamento(@PathVariable UUID apartamentoId) {
        return vagaService.buscarPorApartamento(apartamentoId).stream()
                .map(VagaResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Apenas ADMINISTRADOR pode atualizar vagas (RF09).
     */
    @PutMapping("/{id}")
    public VagaResponseDTO atualizar(
            @PathVariable String id,
            @Valid @RequestBody VagaRequestDTO dto,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        validarAdministrador(role);
        return VagaResponseDTO.fromEntity(vagaService.atualizar(id, dto));
    }

    /**
     * Apenas ADMINISTRADOR pode desativar vagas (RF09).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        validarAdministrador(role);
        vagaService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Apenas ADMINISTRADOR pode reativar vagas (RF09).
     */
    @PostMapping("/{id}/ativar")
    public ResponseEntity<Void> ativar(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        validarAdministrador(role);
        vagaService.ativar(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private void validarAdministrador(String role) {
        if (role == null || !(role.equalsIgnoreCase("ADMINISTRADOR") || role.equalsIgnoreCase("SINDICO"))) {
            throw new OperacaoInvalidaException(
                    "Acesso negado: apenas ADMINISTRADOR ou SINDICO podem executar esta operação.");
        }
    }
}


