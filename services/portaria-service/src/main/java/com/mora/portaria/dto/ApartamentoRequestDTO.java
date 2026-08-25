package com.mora.portaria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ApartamentoRequestDTO {

    @NotBlank(message = "Número do apartamento é obrigatório")
    private String numero;

    @NotNull(message = "Andar é obrigatório")
    private Integer andar;

    @NotNull(message = "Bloco é obrigatório")
    private UUID blocoId;

    private Integer quartos;
    private Double areaMxComTotal;
    private String observacoes;
}

