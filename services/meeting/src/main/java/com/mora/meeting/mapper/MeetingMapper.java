package com.mora.meeting.mapper;

import com.mora.meeting.dto.meeting.MeetingRequestDTO;
import com.mora.meeting.dto.meeting.MeetingResponseDTO;
import com.mora.meeting.entity.Meeting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MeetingMapper {
    Meeting toEntity(MeetingRequestDTO dto);

    @Mapping(source = "meetLink", target = "googleMeetLink")
    MeetingResponseDTO toResponseDto(Meeting meeting);


}