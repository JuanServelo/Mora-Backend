package com.mora.portaria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.mora.portaria.entity.AreaComum;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaComunResponseDTO {
    private String id;
    private String nome;
    private String tipo;
    private String descricao;
    private String localizacao;
    private Integer capacidadeMaxima;
    private Double area;
    private boolean podeReservar;
    private String observacoes;
    private boolean ativo;

    public static AreaComunResponseDTO fromEntity(AreaComum areaComum) {
        return AreaComunResponseDTO.builder()
                .id(areaComum.getId())
                .nome(areaComum.getNome())
                .tipo(areaComum.getTipo())
                .descricao(areaComum.getDescricao())
                .localizacao(areaComum.getLocalizacao())
                .capacidadeMaxima(areaComum.getCapacidadeMaxima())
                .area(areaComum.getArea())
                .podeReservar(areaComum.isPodeReservar())
                .observacoes(areaComum.getObservacoes())
                .ativo(areaComum.isAtivo())
                .build();
    }
}

