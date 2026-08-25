package com.mora.portaria.dto;

import lombok.*;
import com.mora.portaria.entity.Apartamento;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApartamentoResponseDTO {

    private UUID id;
    private String numero;
    private Integer andar;
    private UUID blocoId;
    private String blocoNome;
    private Integer quartos;
    private Double areaMxComTotal;
    private String observacoes;
    private boolean ativo;

    public static ApartamentoResponseDTO fromEntity(Apartamento apartamento) {
        return ApartamentoResponseDTO.builder()
                .id(apartamento.getId())
                .numero(apartamento.getNumero())
                .andar(apartamento.getAndar())
                .blocoId(apartamento.getBloco().getId())
                .blocoNome(apartamento.getBloco().getNome())
                .quartos(apartamento.getQuartos())
                .areaMxComTotal(apartamento.getAreaMxComTotal())
                .observacoes(apartamento.getObservacoes())
                .ativo(apartamento.isAtivo())
                .build();
    }
}
