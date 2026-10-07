package com.mora.meeting.mapper;

import com.mora.meeting.dto.poll.PollRequestDTO;
import com.mora.meeting.dto.poll.PollResponseDTO;
import com.mora.meeting.entity.Poll;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.stream.Collectors;
import com.mora.meeting.entity.PollVote;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PollMapper {

    @Mapping(target = "opcoes", ignore = true)
    Poll toEntity(PollRequestDTO dto);

    @Mapping(target = "usuariosQueVotaram", source = "votos")
    PollResponseDTO toResponseDto(Poll poll);

    @Mapping(target = "quantidadeVotos", expression = "java(option.getVotos() != null ? option.getVotos().size() : 0)")
    com.mora.meeting.dto.poll.PollOptionDTO toOptionDto(com.mora.meeting.entity.PollOption option);

    default List<Long> mapVotos(List<PollVote> votos) {
        if (votos == null) return null;
        return votos.stream().map(PollVote::getUsuarioId).collect(Collectors.toList());
    }
}