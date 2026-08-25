package com.mora.meeting.dto.meeting;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MeetingResponseDTO {

    private Long id;
    private String titulo;
    private String descricao;
    private LocalDateTime dataHoraInicio;
    private LocalDateTime dataHoraFim;

    private String googleMeetLink;
    private String status;
    private Long idOrganizador;
    private List<com.mora.meeting.entity.MeetingGuests> convidados;
}