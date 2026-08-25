package com.mora.portaria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.mora.portaria.entity.ArtigoConhecimento;
import com.mora.portaria.enums.CategoriaArtigo;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtigoConhecimentoResponseDTO {

    private UUID id;
    private String titulo;
    private String conteudo;
    private CategoriaArtigo categoria;
    private String autor;
    private boolean publicado;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static ArtigoConhecimentoResponseDTO fromEntity(ArtigoConhecimento artigo) {
        return ArtigoConhecimentoResponseDTO.builder()
                .id(artigo.getId())
                .titulo(artigo.getTitulo())
                .conteudo(artigo.getConteudo())
                .categoria(artigo.getCategoria())
                .autor(artigo.getAutor())
                .publicado(artigo.isPublicado())
                .criadoEm(artigo.getCriadoEm())
                .atualizadoEm(artigo.getAtualizadoEm())
                .build();
    }
}

