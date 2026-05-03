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
    date = "2026-05-03T18:26:25-0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class PollMapperImpl implements PollMapper {

    @Override
    public Poll toEntity(PollRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Poll.PollBuilder poll = Poll.builder();

        poll.descricao( dto.getDescricao() );
        poll.titulo( dto.getTitulo() );

        return poll.build();
    }

    @Override
    public PollResponseDTO toResponseDto(Poll poll) {
        if ( poll == null ) {
            return null;
        }

        PollResponseDTO.PollResponseDTOBuilder pollResponseDTO = PollResponseDTO.builder();

        pollResponseDTO.meetingId( pollMeetingId( poll ) );
        pollResponseDTO.descricao( poll.getDescricao() );
        pollResponseDTO.id( poll.getId() );
        pollResponseDTO.opcoes( pollOptionListToPollOptionDTOList( poll.getOpcoes() ) );
        pollResponseDTO.status( poll.getStatus() );
        pollResponseDTO.titulo( poll.getTitulo() );

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

        pollOptionDTO.descricao( pollOption.getDescricao() );
        pollOptionDTO.id( pollOption.getId() );

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
