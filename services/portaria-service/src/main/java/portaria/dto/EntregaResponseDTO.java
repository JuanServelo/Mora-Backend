package portaria.dto;

import lombok.Builder;
import lombok.Data;
import portaria.model.Entrega;

@Data
@Builder
public class EntregaResponseDTO {

    private String id;
    private Long   destinatarioId;
    private String destinatarioNome;
    private String bloco;
    private String apartamento;
    private String descricao;
    private String remetente;
    private String status;
    private String observacoes;
    private String dataRecebimento;
    private String dataRetirada;
    private String recebedorNome;

    public static EntregaResponseDTO fromEntity(Entrega e) {
        return EntregaResponseDTO.builder()
                .id(e.getId())
                .destinatarioId(e.getDestinatarioId())
                .destinatarioNome(e.getDestinatarioNome())
                .bloco(e.getBloco())
                .apartamento(e.getApartamento())
                .descricao(e.getDescricao())
                .remetente(e.getRemetente())
                .status(e.getStatus() != null ? e.getStatus() : "PENDENTE")
                .observacoes(e.getObservacoes())
                .dataRecebimento(e.getDataRecebimento() != null
                        ? e.getDataRecebimento().toLocalDate().toString() : null)
                .dataRetirada(e.getDataRetirada() != null
                        ? e.getDataRetirada().toLocalDate().toString() : null)
                .recebedorNome(e.getRecebedorNome())
                .build();
    }
}
