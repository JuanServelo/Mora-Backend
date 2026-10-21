package com.mora.meeting.service;

import com.mora.meeting.dto.meeting.*;
import com.mora.meeting.entity.*;
import com.mora.meeting.enums.*;
import com.mora.meeting.mapper.MeetingMapper;
import com.mora.meeting.repository.MeetingRepository;
import com.mora.meeting.security.*;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MeetingService {
    private final MeetingRepository meetingRepository;
    private final MeetingMapper meetingMapper;
    private final MeetLinkService meetLinks;
    private final AuthApiClient authApi;

    @Transactional
    public MeetingResponseDTO createMeeting(@NotNull MeetingRequestDTO dto) {
        AuthContext.requireManager();
        validate(dto);
        Meeting meeting = meetingMapper.toEntity(dto);
        meeting.setCondominioId(AuthContext.tenant());
        meeting.setIdOrganizador(AuthContext.get().usuarioId());
        meeting.setMeetLink(meetLinks.createLink());
        meeting.setConvidados(new ArrayList<>(dto.getIdConvidados().stream().distinct()
                .map(id -> new MeetingGuests(id, AttendanceStatus.PENDENTE)).toList()));
        meeting.setStatus(MeetingStatus.AGENDADA);
        return meetingMapper.toResponseDto(meetingRepository.save(meeting));
    }

    @Transactional(readOnly = true)
    public MeetingResponseDTO readMeeting(Long id) {
        return meetingMapper.toResponseDto(requireMeeting(id));
    }

    @Transactional(readOnly = true)
    public List<MeetingResponseDTO> listMeetings(Long usuarioId) {
        String tenant = AuthContext.tenant();
        List<Meeting> meetings = AuthContext.isManager() && usuarioId == null
                ? meetingRepository.findByCondominioId(tenant)
                : meetingRepository.findByOrganizadorOrConvidado(tenant,
                    AuthContext.isManager() ? usuarioId : AuthContext.get().usuarioId());
        return meetings.stream().map(meetingMapper::toResponseDto).toList();
    }

    @Transactional
    public MeetingResponseDTO updateMeeting(Long id, MeetingRequestDTO dto) {
        AuthContext.requireManager();
        Meeting meeting = requireMeeting(id);
        validate(dto);
        meeting.setTitulo(dto.getTitulo());
        meeting.setDescricao(dto.getDescricao());
        meeting.setDataHoraInicio(dto.getDataHoraInicio());
        meeting.setDataHoraFim(dto.getDataHoraFim());
        List<MeetingGuests> guests = dto.getIdConvidados().stream().distinct()
                .map(guestId -> meeting.getConvidados().stream().filter(g -> g.getUsuarioId().equals(guestId))
                        .findFirst().orElseGet(() -> new MeetingGuests(guestId, AttendanceStatus.PENDENTE))).toList();
        meeting.getConvidados().clear();
        meeting.getConvidados().addAll(guests);
        return meetingMapper.toResponseDto(meetingRepository.save(meeting));
    }

    @Transactional
    public void cancelMeeting(Long id) {
        AuthContext.requireManager();
        Meeting meeting = requireMeeting(id);
        meeting.setStatus(MeetingStatus.CANCELADA);
        meetingRepository.save(meeting);
    }

    @Transactional
    public void finalizarReuniao(Long id) {
        AuthContext.requireManager();
        Meeting meeting = requireMeeting(id);
        if (meeting.getStatus() == MeetingStatus.FINALIZADA) throw new IllegalStateException("Esta reunião já foi finalizada.");
        if (meeting.getStatus() == MeetingStatus.CANCELADA) throw new IllegalStateException("Reunião cancelada não pode ser finalizada.");
        meeting.setStatus(MeetingStatus.FINALIZADA);
        meetingRepository.save(meeting);
    }

    @Transactional
    public void atualizarStatusPresenca(Long id, Long usuarioId, AttendanceStatus status) {
        AuthContext.requireSelf(usuarioId);
        Meeting meeting = requireMeeting(id);
        if (meeting.getStatus() != MeetingStatus.AGENDADA) throw new IllegalStateException("A reunião não está agendada.");
        guest(meeting, AuthContext.get().usuarioId()).setStatus(status);
        meetingRepository.save(meeting);
    }

    @Transactional
    public void avaliarReuniao(Long id, Long usuarioId, MeetingEvaluationRequestDTO dto) {
        AuthContext.requireSelf(usuarioId);
        Meeting meeting = requireMeeting(id);
        if (meeting.getStatus() != MeetingStatus.FINALIZADA) throw new IllegalStateException("Apenas reuniões finalizadas podem ser avaliadas.");
        MeetingGuests guest = guest(meeting, AuthContext.get().usuarioId());
        if (guest.getStatus() != AttendanceStatus.CONFIRMADO) throw new IllegalStateException("Apenas participantes confirmados podem avaliar.");
        if (guest.getNota() != null) throw new IllegalStateException("Você já enviou uma avaliação.");
        guest.setNota(dto.nota());
        guest.setComentario(dto.comentario());
        meetingRepository.save(meeting);
    }

    @Transactional(readOnly = true)
    public Meeting requireMeeting(Long id) {
        Meeting meeting = meetingRepository.findByIdAndCondominioId(id, AuthContext.tenant())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reunião não encontrada."));
        if (!AuthContext.isManager() && !AuthContext.get().usuarioId().equals(meeting.getIdOrganizador())
                && meeting.getConvidados().stream().noneMatch(g -> g.getUsuarioId().equals(AuthContext.get().usuarioId()))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reunião não encontrada.");
        }
        return meeting;
    }

    private MeetingGuests guest(Meeting meeting, Long id) {
        return meeting.getConvidados().stream().filter(g -> id.equals(g.getUsuarioId())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário não é convidado desta reunião."));
    }
    private void validate(MeetingRequestDTO dto) {
        if (!dto.getDataHoraFim().isAfter(dto.getDataHoraInicio())) {
            throw new IllegalArgumentException("O término deve ser posterior ao início.");
        }
        if (dto.getIdConvidados() == null || dto.getIdConvidados().isEmpty()) {
            throw new IllegalArgumentException("Uma reunião deve ter pelo menos um convidado.");
        }
        authApi.validateGuests(dto.getIdConvidados());
    }
}
