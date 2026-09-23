package portaria.dto.meuveiculo;

import java.util.List;

public record MeusVeiculosResponseDTO(
        List<MeuVeiculoDTO> veiculos,
        List<VagaUnidadeDTO> vagas
) {}
