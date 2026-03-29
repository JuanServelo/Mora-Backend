package com.mora.meeting.controller;

import com.mora.meeting.dto.meeting.MeetingRequestDTO;
import com.mora.meeting.dto.meeting.MeetingResponseDTO;
import com.mora.meeting.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Tag(name = "Reuniões", description = "Endpoints para gerenciamento de agendamentos de reuniões")
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping
    @Operation(summary = "Criar uma nova reunião", description = "Recebe os dados, valida e salva uma nova reunião no banco de dados.")
    public ResponseEntity<MeetingResponseDTO> createMeeting(@Valid @RequestBody MeetingRequestDTO dto) {
        MeetingResponseDTO novaReuniaoDTO = meetingService.createMeeting(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaReuniaoDTO);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver uma reunião", description = "Recebe o id da reunião e retorna as informações sobre ela.")
    public ResponseEntity<MeetingResponseDTO> readMeeting(@PathVariable Long id) {
        MeetingResponseDTO reuniaoDTO = meetingService.readMeeting(id);
        return ResponseEntity.ok(reuniaoDTO);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar uma reunião", description = "Recebe o ID e os novos dados para atualizar uma reunião existente.")
    public ResponseEntity<MeetingResponseDTO> updateMeeting(
            @PathVariable Long id,
            @Valid @RequestBody MeetingRequestDTO dto) {

        MeetingResponseDTO reuniaoAtualizadaDTO = meetingService.updateMeeting(id, dto);
        return ResponseEntity.ok(reuniaoAtualizadaDTO);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancelar uma reunião", description = "Recebe o ID e altera o status da reunião existente para CANCELADA.")
    public ResponseEntity<Void> cancelMeeting(@PathVariable Long id) {
        meetingService.cancelMeeting(id);
        return ResponseEntity.noContent().build();
    }
}