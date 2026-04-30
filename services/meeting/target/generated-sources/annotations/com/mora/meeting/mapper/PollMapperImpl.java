package com.mora.meeting.mapper;

import com.mora.meeting.dto.poll.PollOptionDTO;
import com.mora.meeting.dto.poll.PollRequestDTO;
import com.mora.meeting.dto.poll.PollResponseDTO;
import com.mora.meeting.entity.Meeting;
import com.mora.meeting.entity.Poll;
import com.mora.meeting.entity.PollOption;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-13T17:20:32-0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.1 (Oracle Corporation)"
)
@Component
public class PollMapperImpl implements PollMapper {

    @Override
    public Poll toEntity(PollRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Poll.PollBuilder poll = Poll.builder();

        poll.titulo( dto.getTitulo() );
        poll.descricao( dto.getDescricao() );

        return poll.build();
    }

    @Override
    public PollResponseDTO toResponseDto(Poll poll) {
        if ( poll == null ) {
            return null;
        }

        PollResponseDTO.PollResponseDTOBuilder pollResponseDTO = PollResponseDTO.builder();

        pollResponseDTO.meetingId( pollMeetingId( poll ) );
        pollResponseDTO.id( poll.getId() );
        pollResponseDTO.titulo( poll.getTitulo() );
        pollResponseDTO.descricao( poll.getDescricao() );
        pollResponseDTO.status( poll.getStatus() );
        pollResponseDTO.opcoes( pollOptionListToPollOptionDTOList( poll.getOpcoes() ) );

        return pollResponseDTO.build();
    }

    private Long pollMeetingId(Poll poll) {
        if ( poll == null ) {
            return null;
        }
        Meeting meeting = poll.getMeeting();
        if ( meeting == null ) {
            return null;
        }
        Long id = meeting.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected PollOptionDTO pollOptionToPollOptionDTO(PollOption pollOption) {
        if ( pollOption == null ) {
            return null;
        }

        PollOptionDTO.PollOptionDTOBuilder pollOptionDTO = PollOptionDTO.builder();

        pollOptionDTO.id( pollOption.getId() );
        pollOptionDTO.descricao( pollOption.getDescricao() );

        return pollOptionDTO.build();
    }

    protected List<PollOptionDTO> pollOptionListToPollOptionDTOList(List<PollOption> list) {
        if ( list == null ) {
            return null;
        }

        List<PollOptionDTO> list1 = new ArrayList<PollOptionDTO>( list.size() );
        for ( PollOption pollOption : list ) {
            list1.add( pollOptionToPollOptionDTO( pollOption ) );
        }

        return list1;
    }
}
