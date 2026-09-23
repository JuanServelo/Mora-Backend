package portaria.dto.meuveiculo;

/**
 * Diz à tela o que de fato aconteceu na desvinculação (RN-05):
 * saiu só o vínculo do morador, ou o veículo deixou o cadastro da unidade.
 */
public record DesvincularResultadoDTO(
        String efeito,
        String mensagem
) {
    public static final String VINCULO_REMOVIDO = "VINCULO_REMOVIDO";
    public static final String VEICULO_REMOVIDO = "VEICULO_REMOVIDO";
}
