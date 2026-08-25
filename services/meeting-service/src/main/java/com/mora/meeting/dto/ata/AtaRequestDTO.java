package com.mora.meeting.dto.ata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class AtaRequestDTO {

    @NotBlank(message = "Os tópicos discutidos não podem ficar em branco.")
    private String topicosDiscutidos;

    @NotBlank(message = "As decisões tomadas não podem ficar em branco.")
    private String decisoesTomadas;

    @NotEmpty(message = "A lista de presentes deve conter pelo menos um morador/participante.")
    private List<Long> idPresentes;
}