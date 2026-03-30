package com.mora.meeting.controller;

import com.mora.meeting.dto.ata.AtaRequestDTO;
import com.mora.meeting.dto.ata.AtaResponseDTO;
import com.mora.meeting.service.AtaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meetings/{meetingId}/ata")
@RequiredArgsConstructor
@Tag(name = "Atas", description = "Endpoints para gerenciamento de atas das reuniões")
public class AtaController {

    private final AtaService ataService;

    @PostMapping
    @Operation(summary = "Registrar ata", description = "Registra a ata de uma reunião existente informando os tópicos, decisões e presentes.")
    public ResponseEntity<AtaResponseDTO> registrarAta(
            @PathVariable Long meetingId,
            @Valid @RequestBody AtaRequestDTO dto) {

        AtaResponseDTO novaAta = ataService.registrarAta(meetingId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaAta);
    }

    @PutMapping
    @Operation(summary = "Atualizar ata", description = "Atualiza os dados de uma ata já registrada.")
    public ResponseEntity<AtaResponseDTO> atualizarAta(
            @PathVariable Long meetingId,
            @Valid @RequestBody AtaRequestDTO dto) {

        AtaResponseDTO ataAtualizada = ataService.atualizarAta(meetingId, dto);
        return ResponseEntity.ok(ataAtualizada);
    }

    @GetMapping
    @Operation(summary = "Consultar ata", description = "Retorna a ata vinculada a uma reunião específica.")
    public ResponseEntity<AtaResponseDTO> buscarAta(@PathVariable Long meetingId) {
        AtaResponseDTO ata = ataService.buscarAtaPorReuniao(meetingId);
        return ResponseEntity.ok(ata);
    }
}