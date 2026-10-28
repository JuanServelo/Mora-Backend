package com.mora.meeting.service;

import com.mora.meeting.dto.poll.*;
import com.mora.meeting.entity.*;
import com.mora.meeting.enums.PollStatus;
import com.mora.meeting.mapper.PollMapper;
import com.mora.meeting.repository.*;
import com.mora.meeting.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PollService {
    private final PollRepository pollRepository;
    private final PollMapper pollMapper;
    private final PollVoteRepository pollVoteRepository;
    private final AuthApiClient authApi;

    @Transactional
    public PollResponseDTO createPoll(PollRequestDTO dto) {
        AuthContext.requireManager();
        if (dto.getOpcoes() == null || dto.getOpcoes().size() < 2
                || dto.getOpcoes().stream().anyMatch(o -> o == null || o.isBlank() || o.length() > 100)) {
            throw new IllegalArgumentException("Informe pelo menos duas opções válidas.");
        }
        validateDeadline(dto);
        Poll poll = pollMapper.toEntity(dto);
        poll.setCondominioId(AuthContext.tenant());
        poll.setStatus(PollStatus.ABERTA);
        dto.getOpcoes().forEach(poll::addOption);
        return response(pollRepository.save(poll), authApi.currentUnit());
    }

    @Transactional(readOnly = true)
    public PollResponseDTO readPoll(Long id) {
        return response(find(id), authApi.currentUnit());
    }

    @Transactional(readOnly = true)
    public List<PollResponseDTO> listPollsByCondominio(String ignoredTenant) {
        String unit = authApi.currentUnit();
        return pollRepository.findByCondominioId(AuthContext.tenant()).stream().map(p -> response(p, unit)).toList();
    }

    @Transactional
    public PollResponseDTO updatePoll(Long id, PollRequestDTO dto) {
        AuthContext.requireManager();
        Poll poll = locked(id);
        if (poll.getStatus() != PollStatus.ABERTA || !poll.getVotos().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Votação encerrada ou com votos não pode ser alterada.");
        }
        validateDeadline(dto);
        poll.setTitulo(dto.getTitulo());
        poll.setDescricao(dto.getDescricao());
        poll.setDataHoraFim(dto.getDataHoraFim());
        return response(pollRepository.save(poll), authApi.currentUnit());
    }

    @Transactional
    public void closePoll(Long id) {
        AuthContext.requireManager();
        Poll poll = locked(id);
        poll.setStatus(PollStatus.ENCERRADA);
        pollRepository.save(poll);
    }

    @Transactional
    public void registerVote(Long id, VoteRequestDTO dto) {
        String unit = authApi.currentUnit();
        if (unit == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "É necessário vínculo com uma unidade para votar.");
        Poll poll = locked(id);
        if (poll.getStatus() != PollStatus.ABERTA
                || (poll.getDataHoraFim() != null && !poll.getDataHoraFim().isAfter(LocalDateTime.now()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta votação já foi encerrada.");
        }
        if (pollVoteRepository.existsByPollIdAndUnidadeIdIsNull(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta votação possui votos legados e está disponível apenas para consulta.");
        }
        if (pollVoteRepository.existsByPollIdAndUnidadeId(id, unit)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sua unidade já votou nesta votação.");
        }
        PollOption option = poll.getOpcoes().stream().filter(o -> o.getId().equals(dto.getPollOptionId()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Opção inválida para esta votação."));
        pollVoteRepository.saveAndFlush(PollVote.builder().poll(poll).pollOption(option)
                .usuarioId(AuthContext.get().usuarioId()).unidadeId(unit).build());
    }

    private Poll find(Long id) {
        return pollRepository.findByIdAndCondominioId(id, AuthContext.tenant())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Votação não encontrada."));
    }
    private Poll locked(Long id) {
        return pollRepository.lockByIdAndTenant(id, AuthContext.tenant())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Votação não encontrada."));
    }
    private PollResponseDTO response(Poll poll, String unit) {
        var dto = pollMapper.toResponseDto(poll);
        boolean voted = unit != null && poll.getVotos().stream().anyMatch(v -> unit.equals(v.getUnidadeId()));
        dto.setMinhaUnidadeVotou(voted);
        dto.setPodeVotar(unit != null && !voted && poll.getStatus() == PollStatus.ABERTA
                && (poll.getDataHoraFim() == null || poll.getDataHoraFim().isAfter(LocalDateTime.now()))
                && poll.getVotos().stream().noneMatch(v -> v.getUnidadeId() == null));
        return dto;
    }
    private void validateDeadline(PollRequestDTO dto) {
        if (dto.getDataHoraFim() != null && !dto.getDataHoraFim().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("O prazo da votação deve estar no futuro.");
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60000)
    @Transactional
    public void encerrarVotacoesExpiradasAutomaticamente() {
        var expired = pollRepository.findByStatusAndDataHoraFimBefore(PollStatus.ABERTA, LocalDateTime.now());
        expired.forEach(p -> p.setStatus(PollStatus.ENCERRADA));
        pollRepository.saveAll(expired);
    }
}
