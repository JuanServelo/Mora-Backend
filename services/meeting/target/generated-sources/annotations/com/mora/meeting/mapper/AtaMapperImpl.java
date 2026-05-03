package com.mora.meeting.mapper;

import com.mora.meeting.dto.ata.AtaRequestDTO;
import com.mora.meeting.dto.ata.AtaResponseDTO;
import com.mora.meeting.entity.Ata;
import com.mora.meeting.entity.Meeting;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-03T18:26:24-0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class AtaMapperImpl implements AtaMapper {

    @Override
    public Ata toEntity(AtaRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Ata.AtaBuilder ata = Ata.builder();

        ata.decisoesTomadas( dto.getDecisoesTomadas() );
        List<Long> list = dto.getIdPresentes();
        if ( list != null ) {
            ata.idPresentes( new ArrayList<Long>( list ) );
        }
        ata.topicosDiscutidos( dto.getTopicosDiscutidos() );

        return ata.build();
    }

    @Override
    public AtaResponseDTO toResponseDto(Ata ata) {
        if ( ata == null ) {
            return null;
        }

        AtaResponseDTO.AtaResponseDTOBuilder ataResponseDTO = AtaResponseDTO.builder();

        ataResponseDTO.meetingId( ataMeetingId( ata ) );
        ataResponseDTO.dataPublicacao( ata.getDataPublicacao() );
        ataResponseDTO.decisoesTomadas( ata.getDecisoesTomadas() );
        ataResponseDTO.id( ata.getId() );
        List<Long> list = ata.getIdPresentes();
        if ( list != null ) {
            ataResponseDTO.idPresentes( new ArrayList<Long>( list ) );
        }
        ataResponseDTO.topicosDiscutidos( ata.getTopicosDiscutidos() );

        return ataResponseDTO.build();
    }

    private Long ataMeetingId(Ata ata) {
        if ( ata == null ) {
            return null;
        }
        Meeting meeting = ata.getMeeting();
        if ( meeting == null ) {
            return null;
        }
        Long id = meeting.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
