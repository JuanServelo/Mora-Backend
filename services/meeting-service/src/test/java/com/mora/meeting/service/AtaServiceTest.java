package com.mora.meeting.service;

import com.mora.meeting.dto.ata.AtaRequestDTO;
import com.mora.meeting.dto.ata.AtaResponseDTO;
import com.mora.meeting.entity.Ata;
import com.mora.meeting.entity.Meeting;
import com.mora.meeting.mapper.AtaMapper;
import com.mora.meeting.repository.AtaRepository;
import com.mora.meeting.repository.MeetingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtaServiceTest {

    @Mock
    private AtaRepository ataRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private AtaMapper ataMapper;

    @InjectMocks
    private AtaService ataService;

    private AtaRequestDTO ataRequestDTO;
    private Ata ata;
    private Meeting meeting;
    private AtaResponseDTO ataResponseDTO;

    @BeforeEach
    void setUp() {
        ataRequestDTO = new AtaRequestDTO();
        ataRequestDTO.setTopicosDiscutidos("Tópico 1\nTópico 2");
        ataRequestDTO.setDecisoesTomadas("Decisão 1");
        ataRequestDTO.setIdPresentes(List.of(1L, 2L));

        meeting = Meeting.builder()
                .id(1L)
                .titulo("Reunião Teste")
                .build();

        ata = Ata.builder()
                .id(1L)
                .meeting(meeting)
                .topicosDiscutidos("Tópico 1\nTópico 2")
                .decisoesTomadas("Decisão 1")
                .idPresentes(List.of(1L, 2L))
                .dataPublicacao(LocalDateTime.now())
                .build();

        ataResponseDTO = AtaResponseDTO.builder()
                .id(1L)
                .meetingId(1L)
                .topicosDiscutidos("Tópico 1\nTópico 2")
                .decisoesTomadas("Decisão 1")
                .build();
    }

    @Test
    void registrarAta_Success() {
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(ataRepository.existsByMeetingId(1L)).thenReturn(false);
        when(ataMapper.toEntity(any(AtaRequestDTO.class))).thenReturn(ata);
        when(ataRepository.save(any(Ata.class))).thenReturn(ata);
        when(ataMapper.toResponseDto(any(Ata.class))).thenReturn(ataResponseDTO);

        AtaResponseDTO result = ataService.registrarAta(1L, ataRequestDTO);

        assertNotNull(result);
        assertEquals(1L, result.getMeetingId());
        verify(ataRepository).save(any(Ata.class));
    }

    @Test
    void registrarAta_ThrowsExceptionWhenAtaAlreadyExists() {
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(ataRepository.existsByMeetingId(1L)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ataService.registrarAta(1L, ataRequestDTO);
        });

        assertEquals("Esta reunião já possui uma ata registrada.", exception.getMessage());
        verify(ataRepository, never()).save(any(Ata.class));
    }

    @Test
    void buscarAtaPorReuniao_Success() {
        when(ataRepository.findByMeetingId(1L)).thenReturn(Optional.of(ata));
        when(ataMapper.toResponseDto(any(Ata.class))).thenReturn(ataResponseDTO);

        AtaResponseDTO result = ataService.buscarAtaPorReuniao(1L);

        assertNotNull(result);
        assertEquals(1L, result.getMeetingId());
    }

    @Test
    void atualizarAta_Success() {
        when(ataRepository.findByMeetingId(1L)).thenReturn(Optional.of(ata));
        when(ataRepository.save(any(Ata.class))).thenReturn(ata);
        when(ataMapper.toResponseDto(any(Ata.class))).thenReturn(ataResponseDTO);

        AtaResponseDTO result = ataService.atualizarAta(1L, ataRequestDTO);

        assertNotNull(result);
        verify(ataRepository).save(any(Ata.class));
    }
}
