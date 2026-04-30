package com.mora.meeting.mapper;

import com.mora.meeting.dto.meeting.MeetingRequestDTO;
import com.mora.meeting.dto.meeting.MeetingResponseDTO;
import com.mora.meeting.entity.Meeting;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-13T17:20:32-0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.1 (Oracle Corporation)"
)
@Component
public class MeetingMapperImpl implements MeetingMapper {

    @Override
    public Meeting toEntity(MeetingRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Meeting.MeetingBuilder meeting = Meeting.builder();

        meeting.titulo( dto.getTitulo() );
        meeting.descricao( dto.getDescricao() );
        meeting.dataHoraInicio( dto.getDataHoraInicio() );
        meeting.dataHoraFim( dto.getDataHoraFim() );
        meeting.idOrganizador( dto.getIdOrganizador() );

        return meeting.build();
    }

    @Override
    public MeetingResponseDTO toResponseDto(Meeting meeting) {
        if ( meeting == null ) {
            return null;
        }

        MeetingResponseDTO.MeetingResponseDTOBuilder meetingResponseDTO = MeetingResponseDTO.builder();

        meetingResponseDTO.googleMeetLink( meeting.getMeetLink() );
        meetingResponseDTO.id( meeting.getId() );
        meetingResponseDTO.titulo( meeting.getTitulo() );
        meetingResponseDTO.descricao( meeting.getDescricao() );
        meetingResponseDTO.dataHoraInicio( meeting.getDataHoraInicio() );
        meetingResponseDTO.dataHoraFim( meeting.getDataHoraFim() );
        if ( meeting.getStatus() != null ) {
            meetingResponseDTO.status( meeting.getStatus().name() );
        }

        return meetingResponseDTO.build();
    }
}
