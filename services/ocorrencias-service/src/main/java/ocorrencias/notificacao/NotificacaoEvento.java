package ocorrencias.notificacao;

import java.util.Map;

/**
 * A mensagem que vai para a fila.
 *
 * Os campos são os mesmos da publicação pela rota interna do comunicacao
 * (`NotificacaoService.Publicacao`), para o consumidor tratar as duas entradas
 * do mesmo jeito. `chaveUnica` impede notificar duas vezes o mesmo evento se a
 * mensagem for entregue de novo.
 */
public record NotificacaoEvento(
        String condominioId,
        String usuarioId,
        String origem,
        String tipo,
        String titulo,
        String mensagem,
        Map<String, Object> dados,
        String chaveUnica
) {

    public static final String ORIGEM = "ocorrencias";

    public static final String OCORRENCIA_RESPONDIDA = "OCORRENCIA_RESPONDIDA";
    public static final String ORDEM_SERVICO_ABERTA = "ORDEM_SERVICO_ABERTA";
    public static final String ORDEM_SERVICO_ATRASADA = "ORDEM_SERVICO_ATRASADA";
}
