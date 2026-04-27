package portaria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
