package portaria.dto.meuveiculo;

import portaria.model.enums.StatusAcesso;

import java.util.List;

public record MeuVeiculoDTO(
        String id,
        String placa,
        String modelo,
        String cor,
        String obs,
        String vagaId,
        String vagaNumero,
        StatusAcesso status,
        List<PessoaVinculadaDTO> pessoas
) {
    /** Dentro do condomínio não pode ser desvinculado (RN-05). */
    public boolean dentro() {
        return status == StatusAcesso.DENTRO;
    }
}
