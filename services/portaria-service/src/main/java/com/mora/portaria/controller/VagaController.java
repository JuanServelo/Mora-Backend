package com.mora.portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mora.portaria.dto.VagaResponseDTO;
import com.mora.portaria.entity.Vaga;
import com.mora.portaria.service.VagaService;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/vagas")
@RequiredArgsConstructor
public class VagaController {

    private final VagaService vagaService;

    @PostMapping("/cadastrar")
    public ResponseEntity<VagaResponseDTO> cadastrar(
            @Valid @RequestBody Vaga vaga,
            @RequestParam(required = false) UUID apartamentoId) {
        Vaga salvo = vagaService.cadastrar(vaga, apartamentoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(VagaResponseDTO.fromEntity(salvo));
    }

    @GetMapping
    public List<VagaResponseDTO> listarApenasAtivas() {
        return vagaService.listarApenasAtivas().stream()
                .map(VagaResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/todas")
    public List<VagaResponseDTO> listarTodas() {
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

    @PutMapping("/{id}")
    public VagaResponseDTO atualizar(
            @PathVariable String id,
            @Valid @RequestBody Vaga dados,
            @RequestParam(required = false) String apartamentoId) {
        return VagaResponseDTO.fromEntity(vagaService.atualizar(id, dados, apartamentoId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable String id) {
        vagaService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable String id) {
        vagaService.ativar(id);
        return ResponseEntity.noContent().build();
    }
}

