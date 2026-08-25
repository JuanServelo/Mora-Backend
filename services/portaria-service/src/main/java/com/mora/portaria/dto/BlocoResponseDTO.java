package com.mora.portaria.dto;

import lombok.*;
import com.mora.portaria.entity.Bloco;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlocoResponseDTO {

    private UUID id;
    private String nome;
    private String descricao;
    private Integer andares;
    private Integer apartamentosPorAndar;
    private boolean ativo;

    public static BlocoResponseDTO fromEntity(Bloco bloco) {
        return BlocoResponseDTO.builder()
                .id(bloco.getId())
                .nome(bloco.getNome())
                .descricao(bloco.getDescricao())
                .andares(bloco.getAndares())
                .apartamentosPorAndar(bloco.getApartamentosPorAndar())
                .ativo(bloco.isAtivo())
                .build();
    }
}
