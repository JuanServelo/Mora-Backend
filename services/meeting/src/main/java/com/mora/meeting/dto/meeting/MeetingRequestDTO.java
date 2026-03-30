package com.mora.meeting.dto.meeting;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingRequestDTO {

    @NotBlank(message = "O título é obrigatório")
    @Size(min = 3, max = 100, message = "O título deve ter entre 3 e 100 caracteres")
    private String titulo;

    @Size(max = 500, message = "A descrição não pode ultrapassar 500 caracteres")
    private String descricao;

    @NotNull(message = "A data e hora de início são obrigatórias")
    @FutureOrPresent(message = "A data de início não pode estar no passado")
    private LocalDateTime dataHoraInicio;

    @NotNull(message = "A data e hora de fim são obrigatórias")
    @Future(message = "A data de fim deve ser no futuro")
    private LocalDateTime dataHoraFim;

    @NotNull(message = "O ID do organizador é obrigatório")
    private Long idOrganizador;

    @NotNull(message = "A lista de convidados não pode ser nula")
    private List<Long> idConvidados;

}