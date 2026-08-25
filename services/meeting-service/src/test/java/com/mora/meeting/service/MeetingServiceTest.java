package com.mora.meeting.service;

import com.mora.meeting.dto.meeting.MeetingRequestDTO;
import com.mora.meeting.dto.meeting.MeetingResponseDTO;
import com.mora.meeting.entity.Meeting;
import com.mora.meeting.entity.MeetingGuests;
import com.mora.meeting.enums.AttendanceStatus;
import com.mora.meeting.enums.MeetingStatus;
import com.mora.meeting.mapper.MeetingMapper;
import com.mora.meeting.repository.MeetingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingMapper meetingMapper;

    @InjectMocks
    private MeetingService meetingService;

    private MeetingRequestDTO requestDTO;
    private Meeting meeting;
    private MeetingResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        requestDTO = MeetingRequestDTO.builder()
                .titulo("Reunião Teste")
                .descricao("Descrição da reunião de teste")
                .dataHoraInicio(LocalDateTime.now().plusDays(1))
                .dataHoraFim(LocalDateTime.now().plusDays(1).plusHours(1))
                .idOrganizador(1L)
                .idConvidados(List.of(2L, 3L))
                .build();

        meeting = Meeting.builder()
                .id(1L)
                .titulo("Reunião Teste")
                .descricao("Descrição da reunião de teste")
                .dataHoraInicio(LocalDateTime.now().plusDays(1))
                .dataHoraFim(LocalDateTime.now().plusDays(1).plusHours(1))
                .idOrganizador(1L)
                .status(MeetingStatus.AGENDADA)
                .convidados(new ArrayList<>(List.of(
                        new MeetingGuests(2L, AttendanceStatus.PENDENTE),
                        new MeetingGuests(3L, AttendanceStatus.PENDENTE)
                )))
                .build();

        responseDTO = MeetingResponseDTO.builder()
                .id(1L)
                .titulo("Reunião Teste")
                .descricao("Descrição da reunião de teste")
                .dataHoraInicio(meeting.getDataHoraInicio())
                .dataHoraFim(meeting.getDataHoraFim())
                .idOrganizador(1L)
                .status(MeetingStatus.AGENDADA.name())
                .build();
    }

    @Test
    void createMeeting_Success() {
        when(meetingMapper.toEntity(any(MeetingRequestDTO.class))).thenReturn(meeting);
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);
        when(meetingMapper.toResponseDto(any(Meeting.class))).thenReturn(responseDTO);

        MeetingResponseDTO result = meetingService.createMeeting(requestDTO);

        assertNotNull(result);
        assertEquals("Reunião Teste", result.getTitulo());
        verify(meetingRepository).save(any(Meeting.class));
    }

    @Test
    void createMeeting_ThrowsExceptionWhenEndDateBeforeStartDate() {
        requestDTO.setDataHoraFim(requestDTO.getDataHoraInicio().minusHours(1));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            meetingService.createMeeting(requestDTO);
        });

        assertEquals("A data de término deve ser posterior à data de início.", exception.getMessage());
        verify(meetingRepository, never()).save(any(Meeting.class));
    }

    @Test
    void createMeeting_ThrowsExceptionWhenNoGuests() {
        requestDTO.setIdConvidados(List.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            meetingService.createMeeting(requestDTO);
        });

        assertEquals("Uma reunião deve ter pelo menos um convidado.", exception.getMessage());
        verify(meetingRepository, never()).save(any(Meeting.class));
    }

    @Test
    void readMeeting_Success() {
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(meetingMapper.toResponseDto(any(Meeting.class))).thenReturn(responseDTO);

        MeetingResponseDTO result = meetingService.readMeeting(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void cancelMeeting_Success() {
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);

        meetingService.cancelMeeting(1L);

        assertEquals(MeetingStatus.CANCELADA, meeting.getStatus());
        verify(meetingRepository).save(meeting);
    }
}
