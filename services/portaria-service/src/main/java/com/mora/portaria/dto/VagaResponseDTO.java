package com.mora.portaria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.mora.portaria.entity.Vaga;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VagaResponseDTO {

    private String id;
    private String numero;
    private String localizacao;
    private String tipo;
    private boolean ativa;
    private String apartamentoId;
    private String apartamentoNumero;

    public static VagaResponseDTO fromEntity(Vaga vaga) {
        String aptId = null;
        String aptNumero = null;
        try {
            if (vaga.getApartamento() != null) {
                aptId = vaga.getApartamento().getId().toString();
                aptNumero = vaga.getApartamento().getNumero();
            }
        } catch (Exception ignored) {}

        return VagaResponseDTO.builder()
                .id(vaga.getId())
                .numero(vaga.getNumero())
                .localizacao(vaga.getLocalizacao())
                .tipo(vaga.getTipo())
                .ativa(vaga.isAtiva())
                .apartamentoId(aptId)
                .apartamentoNumero(aptNumero)
                .build();
    }
}

