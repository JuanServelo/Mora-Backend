package com.mora.meeting.service;

import com.mora.meeting.dto.poll.VoteRequestDTO;
import com.mora.meeting.dto.poll.PollRequestDTO;
import com.mora.meeting.dto.poll.PollResponseDTO;
import com.mora.meeting.entity.Meeting;
import com.mora.meeting.entity.Poll;
import com.mora.meeting.entity.PollOption;
import com.mora.meeting.entity.PollVote;
import com.mora.meeting.enums.PollStatus;
import com.mora.meeting.mapper.PollMapper;
import com.mora.meeting.repository.MeetingRepository;
import com.mora.meeting.repository.PollRepository;
import com.mora.meeting.repository.PollVoteRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepository;
    private final MeetingRepository meetingRepository;
    private final PollMapper pollMapper;
    private final PollVoteRepository pollVoteRepository;

    @Transactional
    public PollResponseDTO createPoll(@NotNull PollRequestDTO dto) {

        // Valida se a reunião existe antes de vincular a votação
        Meeting meeting = meetingRepository.findById(dto.getMeetingId())
                .orElseThrow(() -> new RuntimeException("Reunião não encontrada com o ID: " + dto.getMeetingId()));

        Poll poll = pollMapper.toEntity(dto);
        poll.setMeeting(meeting);
        poll.setStatus(PollStatus.ABERTA);

        if (dto.getOpcoes() != null && !dto.getOpcoes().isEmpty()) {
            for (String descricaoOpcao : dto.getOpcoes()) {
                poll.addOption(descricaoOpcao);
            }
        }

        Poll pollSalva = pollRepository.save(poll);

        return pollMapper.toResponseDto(pollSalva);
    }

    @Transactional(readOnly = true)
    public PollResponseDTO readPoll(@NotNull Long id) {
        Poll poll = pollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Votação não encontrada com o ID: " + id));

        return pollMapper.toResponseDto(poll);
    }

    @Transactional
    public PollResponseDTO updatePoll(@NotNull Long id, @NotNull PollRequestDTO dto) {
        Poll pollExistente = pollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Votação não encontrada com o ID: " + id));

        pollExistente.setTitulo(dto.getTitulo());
        pollExistente.setDescricao(dto.getDescricao());

        // Se a regra permitir mudar a votação de reunião, você validaria o novo meetingId aqui.

        Poll pollAtualizada = pollRepository.save(pollExistente);

        return pollMapper.toResponseDto(pollAtualizada);
    }

    @Transactional
    public void closePoll(@NotNull Long id) {
        Poll poll = pollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Votação não encontrada com o ID: " + id));

        poll.setStatus(PollStatus.ENCERRADA);

        // Todo: Adicionar lógica para notificar resultados (se aplicável)

        pollRepository.save(poll);
    }

    @Transactional
    public void registerVote(Long pollId, VoteRequestDTO dto) { // TODO: no futuro, passar o usuarioId extraído do token como parâmetro separadamente

        Long usuarioLogadoId = dto.getUsuarioId();

        /* * TODO (Futuro - Integração Auth):
         * Ao invés de pegar do DTO, você vai pegar do contexto de segurança do Spring (JWT).
         * Exemplo usando Spring Security:
         * * Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
         * Long usuarioLogadoId = jwt.getClaim("id_usuario"); // Depende de como seu colega gerar o payload do token
         */

        // 1. Verifica se a votação existe e está aberta
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new RuntimeException("Votação não encontrada."));

        if (!poll.getStatus().equals(PollStatus.ABERTA)) {
            throw new RuntimeException("Esta votação já foi encerrada.");
        }

        // 2. Verifica se o usuário já votou
        if (pollVoteRepository.existsByPollIdAndUsuarioId(pollId, usuarioLogadoId)) {
            throw new RuntimeException("Usuário já registrou um voto nesta votação.");
        }

        // 3. Verifica se a opção escolhida existe e pertence a esta votação
        PollOption option = poll.getOpcoes().stream()
                .filter(opt -> opt.getId().equals(dto.getPollOptionId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Opção inválida para esta votação."));

        // 4. Salva o voto
        PollVote vote = new PollVote();
        vote.setPoll(poll);
        vote.setPollOption(option);
        vote.setUsuarioId(usuarioLogadoId);

        pollVoteRepository.save(vote);
    }
}