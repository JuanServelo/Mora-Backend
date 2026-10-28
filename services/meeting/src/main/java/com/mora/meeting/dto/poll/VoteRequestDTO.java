package com.mora.meeting.dto.poll;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteRequestDTO {
    @NotNull(message = "O ID da opção escolhida é obrigatório")
    @Positive
    private Long pollOptionId;
}
