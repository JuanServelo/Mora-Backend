package com.mora.meeting.controller;

import com.mora.meeting.dto.meeting.MeetingEvaluationRequestDTO;
import com.mora.meeting.dto.meeting.MeetingRequestDTO;
import com.mora.meeting.dto.meeting.MeetingResponseDTO;
import com.mora.meeting.enums.AttendanceStatus;
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

    @PatchMapping("/{id}/convidados/{usuarioId}/presenca")
    @Operation(summary = "Atualizar presença", description = "Permite que um morador convidado confirme ou recuse a presença na reunião.")
    public ResponseEntity<Void> atualizarPresenca(
            @PathVariable Long id,
            @PathVariable Long usuarioId,
            @RequestParam AttendanceStatus status) {

        meetingService.atualizarStatusPresenca(id, usuarioId, status);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/convidados/{usuarioId}/avaliacoes")
    @Operation(summary = "Avaliar reunião", description = "Permite que um participante avalie a condução da reunião após ela ser finalizada.")
    public ResponseEntity<Void> avaliarReuniao(
            @PathVariable Long id,
            @PathVariable Long usuarioId,
            @Valid @RequestBody MeetingEvaluationRequestDTO dto) {

        meetingService.avaliarReuniao(id, usuarioId, dto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/finalizar")
    public ResponseEntity<Void> finalizarReuniao(@PathVariable Long id) {
        meetingService.finalizarReuniao(id);
        return ResponseEntity.noContent().build();
    }
}