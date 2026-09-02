package com.mora.meeting.mapper;

import com.mora.meeting.dto.meeting.MeetingRequestDTO;
import com.mora.meeting.dto.meeting.MeetingResponseDTO;
import com.mora.meeting.entity.Meeting;
import com.mora.meeting.entity.MeetingGuests;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-06T15:27:22-0300",
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
        List<MeetingGuests> list = meeting.getConvidados();
        if ( list != null ) {
            meetingResponseDTO.convidados( new ArrayList<MeetingGuests>( list ) );
        }
        meetingResponseDTO.dataHoraFim( meeting.getDataHoraFim() );
        meetingResponseDTO.dataHoraInicio( meeting.getDataHoraInicio() );
        meetingResponseDTO.descricao( meeting.getDescricao() );
        meetingResponseDTO.id( meeting.getId() );
        meetingResponseDTO.idOrganizador( meeting.getIdOrganizador() );
        if ( meeting.getStatus() != null ) {
            meetingResponseDTO.status( meeting.getStatus().name() );
        }
        meetingResponseDTO.titulo( meeting.getTitulo() );

        return meetingResponseDTO.build();
    }
}
