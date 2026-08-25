package com.mora.portaria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.mora.portaria.enums.PublicoAlvo;

@Data
public class AvisoRequestDTO {

    @NotBlank(message = "Título é obrigatório")
    private String titulo;

    @NotBlank(message = "Mensagem é obrigatória")
    private String mensagem;

    // Datas em texto ISO (yyyy-MM-dd) — convertidas para LocalDate no service.
    @NotBlank(message = "Data inicial é obrigatória")
    private String dataInicio;

    @NotBlank(message = "Data final é obrigatória")
    private String dataFim;

    @NotNull(message = "Público-alvo é obrigatório")
    private PublicoAlvo publicoAlvo;

    private String condominioId;

    private String autor;

    private boolean publicado;
}

