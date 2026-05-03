package com.mora.meeting.mapper;

import com.mora.meeting.dto.meeting.MeetingRequestDTO;
import com.mora.meeting.dto.meeting.MeetingResponseDTO;
import com.mora.meeting.entity.Meeting;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-03T18:26:25-0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class MeetingMapperImpl implements MeetingMapper {

    @Override
    public Meeting toEntity(MeetingRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Meeting.MeetingBuilder meeting = Meeting.builder();

        meeting.dataHoraFim( dto.getDataHoraFim() );
        meeting.dataHoraInicio( dto.getDataHoraInicio() );
        meeting.descricao( dto.getDescricao() );
        meeting.idOrganizador( dto.getIdOrganizador() );
        meeting.titulo( dto.getTitulo() );

        return meeting.build();
    }

    @Override
    public MeetingResponseDTO toResponseDto(Meeting meeting) {
        if ( meeting == null ) {
            return null;
        }

        MeetingResponseDTO.MeetingResponseDTOBuilder meetingResponseDTO = MeetingResponseDTO.builder();

        meetingResponseDTO.googleMeetLink( meeting.getMeetLink() );
        meetingResponseDTO.dataHoraFim( meeting.getDataHoraFim() );
        meetingResponseDTO.dataHoraInicio( meeting.getDataHoraInicio() );
        meetingResponseDTO.descricao( meeting.getDescricao() );
        meetingResponseDTO.id( meeting.getId() );
        if ( meeting.getStatus() != null ) {
            meetingResponseDTO.status( meeting.getStatus().name() );
        }
        meetingResponseDTO.titulo( meeting.getTitulo() );

        return meetingResponseDTO.build();
    }
}
