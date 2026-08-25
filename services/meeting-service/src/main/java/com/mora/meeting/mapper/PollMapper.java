package com.mora.meeting.mapper;

import com.mora.meeting.dto.poll.PollRequestDTO;
import com.mora.meeting.dto.poll.PollResponseDTO;
import com.mora.meeting.entity.Poll;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PollMapper {

    @Mapping(target = "opcoes", ignore = true)
    Poll toEntity(PollRequestDTO dto);

    @Mapping(source = "meeting.id", target = "meetingId")
    PollResponseDTO toResponseDto(Poll poll);
}