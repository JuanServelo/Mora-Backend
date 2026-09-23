package portaria.dto.meuveiculo;

/**
 * Situação de uma vaga da unidade (RN-02, RN-07).
 *
 * `ocupada` é derivada de quem está dentro agora, não do vínculo: uma vaga
 * atribuída a um veículo que saiu aparece como livre.
 */
public record VagaUnidadeDTO(
        String id,
        String numero,
        String localizacao,
        boolean ocupada,
        String ocupadaPorPlaca,
        String ocupadaPorNome,
        /** MORADOR ou VISITANTE — visitante é somente leitura para o morador. */
        String origem
) {}
