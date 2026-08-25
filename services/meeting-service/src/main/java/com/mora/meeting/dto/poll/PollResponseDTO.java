package com.mora.meeting.dto.poll;

import com.mora.meeting.enums.PollStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PollResponseDTO {

    private Long id;
    private String titulo;
    private String descricao;
    private Long meetingId;
    private PollStatus status;
    private List<PollOptionDTO> opcoes;
}