package portaria.dto;

import lombok.*;
import portaria.model.Bloco;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlocoResponseDTO {

    private UUID id;
    private String nome;
    private String sigla;
    private String descricao;
    private Integer andares;
    private Integer apartamentosPorAndar;
    private boolean ativo;

    public static BlocoResponseDTO fromEntity(Bloco bloco) {
        return BlocoResponseDTO.builder()
                .id(bloco.getId())
                .nome(bloco.getNome())
                .sigla(bloco.getSigla())
                .descricao(bloco.getDescricao())
                .andares(bloco.getAndares())
                .apartamentosPorAndar(bloco.getApartamentosPorAndar())
                .ativo(bloco.isAtivo())
                .build();
    }
}