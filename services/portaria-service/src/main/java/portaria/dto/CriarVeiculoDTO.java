package portaria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import portaria.model.enums.CategoriaVeiculo;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriarVeiculoDTO {

    @NotBlank(message = "Placa é obrigatória")
    private String placa;

    private String modelo;

    @NotNull(message = "Categoria do veículo é obrigatória")
    private CategoriaVeiculo categoria;

    /** Obrigatório para CARRO e MOTO; ignorado para VEICULO_SERVICO */
    private String proprietarioId;

    /** Obrigatório para CARRO e MOTO; ignorado para VEICULO_SERVICO */
    private String vagaId;
}
