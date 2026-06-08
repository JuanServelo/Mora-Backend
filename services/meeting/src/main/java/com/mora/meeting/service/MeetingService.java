package com.mora.meeting.service;

import com.google.api.services.meet.v2.model.Space;
import com.mora.meeting.dto.meeting.MeetingEvaluationRequestDTO;
import com.mora.meeting.dto.meeting.MeetingRequestDTO;
import com.mora.meeting.dto.meeting.MeetingResponseDTO;
import com.mora.meeting.entity.Meeting;
import com.mora.meeting.entity.MeetingGuests;
import com.mora.meeting.enums.AttendanceStatus;
import com.mora.meeting.enums.MeetingStatus;
import com.mora.meeting.mapper.MeetingMapper;
import com.mora.meeting.repository.MeetingRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.google.api.services.meet.v2.Meet;


import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingMapper meetingMapper;
    //private final Meet googleMeetClient;

    @Transactional
    public MeetingResponseDTO createMeeting(@NotNull MeetingRequestDTO dto) {

        if (dto.getDataHoraFim().isBefore(dto.getDataHoraInicio())) {
            throw new IllegalArgumentException("A data de término deve ser posterior à data de início.");
        }

        Meeting meeting = meetingMapper.toEntity(dto);

        //todo: alterar para microsserviço de usuários
        List<String> emailsParaOMeet = buscarEmailsDosUsuariosMock(dto.getIdConvidados());
        System.out.println("E-mails recuperados para o convite: " + emailsParaOMeet);


        /* * acesso a microserviço de usuários
         * List<String> emails = usuarioClient.buscarEmails(dto.getIdConvidados());
         */


//        try {
//            Space spaceRequest = new Space();
//            Space createdSpace = googleMeetClient.spaces().create(spaceRequest).execute();
//            String meetLink = createdSpace.getMeetingUri();
//            meeting.setMeetLink(meetLink);
//
//            System.out.println("Link do Meet gerado com sucesso: " + meetLink);
//
//        } catch (Exception e) {
//            System.err.println("Erro ao comunicar com a API do Google Meet: " + e.getMessage());
//            throw new RuntimeException("Falha ao gerar o link da reunião no Google Meet.", e);
//        }

        // Link fake só para o banco não dar erro de nulo (caso seja obrigatório)
        meeting.setMeetLink("https://meet.google.com/fake-link-temporario");

        List<MeetingGuests> convidados = dto.getIdConvidados().stream()
                .map(id -> new MeetingGuests(id, AttendanceStatus.PENDENTE))
                .toList();
        meeting.setConvidados(convidados);

        meeting.setStatus(MeetingStatus.AGENDADA);
        Meeting meetingSalvo = meetingRepository.save(meeting);

        return meetingMapper.toResponseDto(meetingSalvo);
    }

    @Transactional(readOnly = true)
    public MeetingResponseDTO readMeeting(@NotNull Long id) {

        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reunião não encontrada com o ID: " + id));

        MeetingResponseDTO responseDTO = meetingMapper.toResponseDto(meeting);

        /* * acesso a microserviço de usuários
         * * List<String> emails = buscarEmailsDosUsuariosMock(meeting.getIdConvidados());
         * responseDTO.setConvidados(emails);
         */

        return responseDTO;
    }

    @Transactional(readOnly = true)
    public List<MeetingResponseDTO> listMeetings(Long usuarioId) {
        List<Meeting> meetings;
        if (usuarioId != null) {
            meetings = meetingRepository.findByOrganizadorOrConvidado(usuarioId);
        } else {
            meetings = meetingRepository.findAll();
        }
        return meetings.stream()
                .map(meetingMapper::toResponseDto)
                .toList();
    }

    @Transactional
    public MeetingResponseDTO updateMeeting(@NotNull Long id, @NotNull MeetingRequestDTO dto) {
        if (dto.getDataHoraFim().isBefore(dto.getDataHoraInicio())) {
            throw new IllegalArgumentException("A data de término deve ser posterior à data de início.");
        }
        Meeting meetingExistente = meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reunião não encontrada com o ID: " + id));

        meetingExistente.setTitulo(dto.getTitulo());
        meetingExistente.setDescricao(dto.getDescricao());
        meetingExistente.setDataHoraInicio(dto.getDataHoraInicio());
        meetingExistente.setDataHoraFim(dto.getDataHoraFim());

        // Atualiza a lista de convidados mantendo o status dos que já estavam presentes
        List<com.mora.meeting.entity.MeetingGuests> novosConvidados = dto.getIdConvidados().stream()
                .map(convidadoId -> {
                    com.mora.meeting.entity.MeetingGuests anterior = meetingExistente.getConvidados().stream()
                            .filter(c -> c.getUsuarioId().equals(convidadoId))
                            .findFirst()
                            .orElse(null);
                    return new com.mora.meeting.entity.MeetingGuests(convidadoId, anterior != null ? anterior.getStatus() : com.mora.meeting.enums.AttendanceStatus.PENDENTE);
                })
                .toList();
        
        meetingExistente.getConvidados().clear();
        meetingExistente.getConvidados().addAll(novosConvidados);

        Meeting meetingAtualizado = meetingRepository.save(meetingExistente);

        return meetingMapper.toResponseDto(meetingAtualizado);
    }

    @Transactional
    public void cancelMeeting(@NotNull Long id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reunião não encontrada com o ID: " + id));

        meeting.setStatus(MeetingStatus.CANCELADA);

        // Todo : Adicionar lógica para cancelar a reunião no Google Meet
        // Todo : Disparar notificação/e-mail de cancelamento para os convidados

        meetingRepository.save(meeting);
    }

    @Transactional
    public void atualizarStatusPresenca(Long meetingId, Long usuarioId, AttendanceStatus novoStatus) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Reunião não encontrada com o ID: " + meetingId));

        boolean convidadoAtualizado = false;

        for (MeetingGuests convidado : meeting.getConvidados()) {
            if (convidado.getUsuarioId().equals(usuarioId)) {
                convidado.setStatus(novoStatus);
                convidadoAtualizado = true;
                break;
            }
        }

        if (!convidadoAtualizado) {
            throw new IllegalArgumentException("O usuário informado não é um convidado desta reunião.");
        }

        meetingRepository.save(meeting);
    }

    @Transactional
    public void avaliarReuniao(Long meetingId, Long usuarioId, MeetingEvaluationRequestDTO dto) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Reunião não encontrada com o ID: " + meetingId));

        if (meeting.getStatus() != MeetingStatus.FINALIZADA) {
            throw new IllegalStateException("Apenas reuniões finalizadas podem ser avaliadas.");
        }
        boolean achouConvidado = false;
        for (MeetingGuests convidado : meeting.getConvidados()) {
            if (convidado.getUsuarioId().equals(usuarioId)) {
                achouConvidado = true;

                if (convidado.getStatus() != AttendanceStatus.CONFIRMADO) {
                    throw new IllegalStateException("Apenas participantes com presença confirmada podem avaliar a reunião.");
                }
                if (convidado.getNota() != null) {
                    throw new IllegalStateException("Você já enviou uma avaliação para esta reunião.");
                }

                convidado.setNota(dto.nota());
                convidado.setComentario(dto.comentario());
                break;
            }
        }
        if (!achouConvidado) {
            throw new IllegalArgumentException("O usuário informado não é um convidado desta reunião.");
        }
        meetingRepository.save(meeting);
    }

    @Transactional
    public void finalizarReuniao(Long id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reunião não encontrada."));

        // Opcional: Validar se já não está finalizada para evitar processamento à toa
        if (MeetingStatus.FINALIZADA.equals(meeting.getStatus())) {
            throw new IllegalStateException("Esta reunião já foi finalizada.");
        }

        meeting.setStatus(MeetingStatus.FINALIZADA);

        // Futuramente, aqui você poderia disparar um evento:
        // eventPublisher.publishEvent(new MeetingFinishedEvent(meeting));

        meetingRepository.save(meeting);
    }

    //MOCK DE USUÁRIOS
    private List<String> buscarEmailsDosUsuariosMock(List<Long> ids) {
        Map<Long, String> bancoDeUsuariosFalso = Map.of(
                1L, "akemiskrd@gmail.com",
                2L, "juan@empresa.com",
                3L, "joao.victor@empresa.com",
                4L, "thais@empresa.com",
                5L, "ray@empresa.com"
        );

        // Percorre a lista de IDs que chegou no DTO e busca o e-mail correspondente
        return ids.stream()
                .map(id -> bancoDeUsuariosFalso.getOrDefault(id, "convidado_padrao@empresa.com"))
                .toList();
    }
}