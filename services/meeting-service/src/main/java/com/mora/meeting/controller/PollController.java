package com.mora.meeting.controller;

import com.mora.meeting.dto.poll.PollRequestDTO;
import com.mora.meeting.dto.poll.PollResponseDTO;
import com.mora.meeting.dto.poll.VoteRequestDTO;
import com.mora.meeting.service.PollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
@Tag(name = "Votações", description = "Endpoints para criar e gerenciar votações vinculadas a reuniões")
public class PollController {

    private final PollService pollService;

    @PostMapping
    @Operation(summary = "Criar uma nova votação", description = "Recebe os dados, vincula a uma reunião existente e salva a votação.")
    public ResponseEntity<PollResponseDTO> createPoll(@Valid @RequestBody PollRequestDTO dto) {
        PollResponseDTO novaVotacaoDTO = pollService.createPoll(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaVotacaoDTO);
    }

    @GetMapping
    @Operation(summary = "Listar votações", description = "Lista votações filtrando por reunião ou por data.")
    public ResponseEntity<java.util.List<PollResponseDTO>> listPolls(
            @RequestParam(required = false) Long meetingId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        if (meetingId != null) {
            return ResponseEntity.ok(pollService.listPollsByMeeting(meetingId));
        } else if (date != null) {
            return ResponseEntity.ok(pollService.listPollsByDate(date));
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver uma votação", description = "Recebe o ID da votação e retorna as informações detalhadas.")
    public ResponseEntity<PollResponseDTO> readPoll(@PathVariable Long id) {
        PollResponseDTO votacaoDTO = pollService.readPoll(id);
        return ResponseEntity.ok(votacaoDTO);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar uma votação", description = "Recebe o ID e atualiza os dados básicos de uma votação.")
    public ResponseEntity<PollResponseDTO> updatePoll(
            @PathVariable Long id,
            @Valid @RequestBody PollRequestDTO dto) {
        PollResponseDTO votacaoAtualizadaDTO = pollService.updatePoll(id, dto);
        return ResponseEntity.ok(votacaoAtualizadaDTO);
    }

    @PatchMapping("/{id}/close")
    @Operation(summary = "Encerrar uma votação", description = "Muda o status da votação para ENCERRADA, bloqueando novos votos.")
    public ResponseEntity<Void> closePoll(@PathVariable Long id) {
        pollService.closePoll(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/votes")
    @Operation(summary = "Registrar um voto", description = "Computa o voto de um usuário em uma opção específica da votação.")
    public ResponseEntity<Void> registerVote(
            @PathVariable Long id,
            @Valid @RequestBody VoteRequestDTO dto) {

        /* * TODO (Futuro - Integração Auth):
         * Se vocês usarem o @AuthenticationPrincipal do Spring Security, a assinatura do método ficaria assim:
         * * public ResponseEntity<Void> registerVote(
         * @PathVariable Long id,
         * @Valid @RequestBody VoteRequestDTO dto,
         * @AuthenticationPrincipal Jwt jwt) { // Pega o token automaticamente
         * * Long usuarioId = jwt.getClaim("id_usuario");
         * pollService.registerVote(id, dto.getPollOptionId(), usuarioId); // Mudaria a assinatura do service
         * }
         */

        pollService.registerVote(id, dto);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}