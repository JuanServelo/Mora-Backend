package comunicacao.model.enums;

public enum TipoNotificacao {
    AVISO,
    ENTREGA,
    RESERVA,
    ALUGUEL_VAGA,
    PRE_AUTORIZACAO,
    CHAT,
    SISTEMA,

    // Publicados pelo financeiro pela rota interna. Os nomes são os que ele já
    // manda; o frontend (`utils/notificacoes.js`) decide ícone e destino por eles.
    NOVA_FATURA,
    FATURA_VENCIDA,
    PAGAMENTO_CONFIRMADO,
    MULTA_APLICADA,
    MULTA_JULGADA,
    PAGAMENTO_ESTORNADO,

    // Publicados pelo ocorrencias-service pela fila `mora.notificacoes`.
    OCORRENCIA_RESPONDIDA,
    ORDEM_SERVICO_ABERTA,
    ORDEM_SERVICO_ATRASADA
}
