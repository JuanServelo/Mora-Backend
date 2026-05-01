package portaria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import portaria.model.enums.TipoProprietario;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO {

    private String id;
    private String nome;
    private String cpf;
    private String email;
    private String telefone;
    private TipoProprietario tipo;
    private boolean ativo;
}
