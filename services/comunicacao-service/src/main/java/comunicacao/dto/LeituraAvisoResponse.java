package comunicacao.dto;

import java.util.List;

/**
 * Estatística de leitura de um aviso.
 *
 * O percentual só faz sentido com o denominador: total de destinatários vem do
 * auth-api, filtrado pelo público-alvo do aviso. Quando o auth-api não responde,
 * `totalDestinatarios` vem zerado e o percentual, nulo — melhor omitir o
 * indicador do que exibir um número errado.
 */
public record LeituraAvisoResponse(
        String avisoId,
        long totalDestinatarios,
        long totalLeituras,
        Double percentual,
        List<LeitorResponse> leitores,
        List<UsuarioResumo> pendentes
) {}
