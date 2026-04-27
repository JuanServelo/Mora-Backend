package portaria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import portaria.model.enums.TipoProprietario;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriarCarroDTO {

    @NotBlank(message = "Placa é obrigatória")
    private String placa;

    private String modelo;

    @NotNull(message = "ID do proprietário é obrigatório")
    private String proprietarioId;

    @NotNull(message = "Tipo de proprietário é obrigatório")
    private TipoProprietario tipoProprietario;
}
