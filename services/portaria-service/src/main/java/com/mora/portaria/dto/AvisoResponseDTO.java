package com.mora.portaria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.mora.portaria.entity.Aviso;
import com.mora.portaria.enums.PublicoAlvo;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvisoResponseDTO {

    private UUID id;
    private String titulo;
    private String mensagem;
    private String dataInicio;
    private String dataFim;
    private PublicoAlvo publicoAlvo;
    private String condominioId;
    private String autor;
    private boolean publicado;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static AvisoResponseDTO fromEntity(Aviso aviso) {
        return AvisoResponseDTO.builder()
                .id(aviso.getId())
                .titulo(aviso.getTitulo())
                .mensagem(aviso.getMensagem())
                .dataInicio(aviso.getDataInicio() != null ? aviso.getDataInicio().toString() : null)
                .dataFim(aviso.getDataFim() != null ? aviso.getDataFim().toString() : null)
                .publicoAlvo(aviso.getPublicoAlvo())
                .condominioId(aviso.getCondominioId())
                .autor(aviso.getAutor())
                .publicado(aviso.isPublicado())
                .criadoEm(aviso.getCriadoEm())
                .atualizadoEm(aviso.getAtualizadoEm())
                .build();
    }
}

