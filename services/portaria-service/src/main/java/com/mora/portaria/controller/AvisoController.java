package com.mora.portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mora.portaria.dto.AvisoRequestDTO;
import com.mora.portaria.dto.AvisoResponseDTO;
import com.mora.portaria.service.AvisoService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/avisos")
@RequiredArgsConstructor
public class AvisoController {

    private final AvisoService service;

    @PostMapping
    public ResponseEntity<AvisoResponseDTO> criar(@Valid @RequestBody AvisoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AvisoResponseDTO.fromEntity(service.criar(request)));
    }

    @PutMapping("/{id}")
    public AvisoResponseDTO atualizar(@PathVariable UUID id, @Valid @RequestBody AvisoRequestDTO request) {
        return AvisoResponseDTO.fromEntity(service.atualizar(id, request));
    }

    @PatchMapping("/{id}/encerrar")
    public AvisoResponseDTO encerrar(@PathVariable UUID id) {
        return AvisoResponseDTO.fromEntity(service.encerrar(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    // Todos os avisos do condomínio (visão admin).
    @GetMapping
    public List<AvisoResponseDTO> listar(@RequestParam(required = false) String condominioId) {
        return service.listarPorCondominio(condominioId).stream()
                .map(AvisoResponseDTO::fromEntity)
                .toList();
    }

    // Avisos vigentes (visão do morador, para a Home).
    @GetMapping("/ativos")
    public List<AvisoResponseDTO> listarAtivos(@RequestParam String condominioId) {
        return service.listarAtivos(condominioId).stream()
                .map(AvisoResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public AvisoResponseDTO buscarPorId(@PathVariable UUID id) {
        return AvisoResponseDTO.fromEntity(service.buscarPorId(id));
    }
}

