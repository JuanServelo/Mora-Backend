package portaria.dto.meuveiculo;

import java.util.List;

public record CadastrarMeuVeiculoDTO(
        String placa,
        String modelo,
        String cor,
        String obs,
        String vagaId,
        /** Ao menos uma pessoa da unidade; sem vínculo o veículo nasceria órfão. */
        List<String> pessoaIds
) {}
