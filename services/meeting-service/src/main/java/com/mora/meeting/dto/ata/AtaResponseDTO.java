package com.mora.meeting.dto.ata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtaResponseDTO {

    private Long id;
    private Long meetingId;
    private String topicosDiscutidos;
    private String decisoesTomadas;
    private List<Long> idPresentes;
    private LocalDateTime dataPublicacao;

}