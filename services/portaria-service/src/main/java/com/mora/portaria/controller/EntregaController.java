package com.mora.portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mora.portaria.dto.EntregaRequestDTO;
import com.mora.portaria.dto.EntregaResponseDTO;
import com.mora.portaria.service.EntregaService;

import java.util.List;

@RestController
@RequestMapping("/entregas")
@RequiredArgsConstructor
public class EntregaController {

    private final EntregaService entregaService;

    @PostMapping("/cadastrar")
    public ResponseEntity<EntregaResponseDTO> cadastrar(@RequestBody EntregaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EntregaResponseDTO.fromEntity(entregaService.cadastrar(dto)));
    }

    @PutMapping("/{id}")
    public EntregaResponseDTO atualizar(@PathVariable String id, @RequestBody EntregaRequestDTO dto) {
        return EntregaResponseDTO.fromEntity(entregaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        entregaService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<EntregaResponseDTO> listarTodas() {
        return entregaService.listarTodas().stream()
                .map(EntregaResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/pendentes")
    public List<EntregaResponseDTO> listarPendentes() {
        return entregaService.listarPendentes().stream()
                .map(EntregaResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public EntregaResponseDTO buscarPorId(@PathVariable String id) {
        return EntregaResponseDTO.fromEntity(entregaService.buscarPorId(id));
    }

    // Endpoint legado da portaria: marcar retirada com nome do recebedor
    @PostMapping("/{id}/retirar")
    public EntregaResponseDTO retirar(@PathVariable String id, @RequestBody(required = false) String recebedor) {
        return EntregaResponseDTO.fromEntity(entregaService.retirar(id, recebedor));
    }
}

