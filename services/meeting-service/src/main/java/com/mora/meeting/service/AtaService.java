package com.mora.meeting.service;

import com.mora.meeting.dto.ata.AtaRequestDTO;
import com.mora.meeting.dto.ata.AtaResponseDTO;
import com.mora.meeting.entity.Ata;
import com.mora.meeting.entity.Meeting;
import com.mora.meeting.repository.AtaRepository;
import com.mora.meeting.repository.MeetingRepository;
import com.mora.meeting.mapper.AtaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AtaService {

    private final AtaRepository ataRepository;
    private final MeetingRepository meetingRepository;
     private final AtaMapper ataMapper;

    @Transactional
    public AtaResponseDTO registrarAta(Long meetingId, AtaRequestDTO dto) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Reunião não encontrada"));

        if (ataRepository.existsByMeetingId(meetingId)) {
            throw new RuntimeException("Esta reunião já possui uma ata registrada.");
        }

        Ata novaAta = ataMapper.toEntity(dto);

        novaAta.setMeeting(meeting);
        novaAta.setDataPublicacao(LocalDateTime.now());

        Ata ataSalva = ataRepository.save(novaAta);

        return ataMapper.toResponseDto(ataSalva);
    }

    @Transactional
    public AtaResponseDTO atualizarAta(Long meetingId, AtaRequestDTO dto) {
        Ata ataAtual = ataRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new RuntimeException("Ata não encontrada para esta reunião"));

        ataAtual.setTopicosDiscutidos(dto.getTopicosDiscutidos());
        ataAtual.setDecisoesTomadas(dto.getDecisoesTomadas());
        ataAtual.setIdPresentes(dto.getIdPresentes());

        Ata ataAtualizada = ataRepository.save(ataAtual);

        return ataMapper.toResponseDto(ataAtualizada);
    }

    @Transactional(readOnly = true)
    public AtaResponseDTO buscarAtaPorReuniao(Long meetingId) {
        Ata ata = ataRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new RuntimeException("Ata não encontrada para a reunião com ID: " + meetingId));

        return ataMapper.toResponseDto(ata);
    }
}