package com.mora.meeting.mapper;

import com.mora.meeting.dto.ata.AtaRequestDTO;
import com.mora.meeting.dto.ata.AtaResponseDTO;
import com.mora.meeting.entity.Ata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AtaMapper {

    Ata toEntity(AtaRequestDTO dto);

    // Ensina o MapStruct a extrair o ID do objeto Meeting e colocar no DTO
    @Mapping(source = "meeting.id", target = "meetingId")
    AtaResponseDTO toResponseDto(Ata ata);
}