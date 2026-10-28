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
@Tag(name = "Votações", description = "Endpoints para criar e gerenciar votações independentes")
public class PollController {

    private final PollService pollService;

    @PostMapping
    @Operation(summary = "Criar uma nova votação", description = "Recebe os dados e salva a votação.")
    public ResponseEntity<PollResponseDTO> createPoll(@Valid @RequestBody PollRequestDTO dto) {
        PollResponseDTO novaVotacaoDTO = pollService.createPoll(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaVotacaoDTO);
    }

    @GetMapping
    @Operation(summary = "Listar votações", description = "Lista votações do condomínio do JWT. O parâmetro condominioId não altera o escopo.")
    public ResponseEntity<java.util.List<PollResponseDTO>> listPolls(
            @RequestParam(required = false) String condominioId) {
        return ResponseEntity.ok(pollService.listPollsByCondominio(condominioId));
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
    @Operation(summary = "Registrar voto da unidade", description = "Obtém o usuário do JWT e a unidade no auth-api. Permite um voto por unidade, sem aceitar identidade no corpo.")
    public ResponseEntity<Void> registerVote(
            @PathVariable Long id,
            @Valid @RequestBody VoteRequestDTO dto) {

        pollService.registerVote(id, dto);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
