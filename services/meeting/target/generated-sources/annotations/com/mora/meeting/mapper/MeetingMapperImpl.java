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
    date = "2026-08-26T14:47:01-0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
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
        meetingResponseDTO.idOrganizador( meeting.getIdOrganizador() );
        List<MeetingGuests> list = meeting.getConvidados();
        if ( list != null ) {
            meetingResponseDTO.convidados( new ArrayList<MeetingGuests>( list ) );
        }

        return meetingResponseDTO.build();
    }
}
