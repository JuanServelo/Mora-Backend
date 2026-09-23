package portaria.dto.meuveiculo;

/**
 * Campos que o morador edita (RN-04).
 *
 * Placa fica de fora de propósito: alterá-la transformaria o registro em outro
 * veículo e o histórico de acesso passaria a apontar para o carro errado.
 */
public record AtualizarMeuVeiculoDTO(
        String modelo,
        String cor,
        String obs,
        String vagaId
) {}
