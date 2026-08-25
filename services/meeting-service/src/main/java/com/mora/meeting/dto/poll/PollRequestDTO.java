package com.mora.meeting.dto.poll;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollRequestDTO {

    @NotBlank(message = "O título da votação é obrigatório")
    @Size(min = 3, max = 100, message = "O título deve ter entre 3 e 100 caracteres")
    private String titulo;

    @Size(max = 500, message = "A descrição não pode ultrapassar 500 caracteres")
    private String descricao;

    @NotNull(message = "O ID da reunião vinculada é obrigatório")
    private Long meetingId;

    private List<String> opcoes;
}