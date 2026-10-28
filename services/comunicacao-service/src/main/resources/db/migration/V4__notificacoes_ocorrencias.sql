-- =============================================================================
-- V4: notificações do ocorrencias-service
--
-- O ocorrencias-service publica, pela fila `mora.notificacoes` (RabbitMQ), três
-- tipos novos: ocorrência respondida, ordem de serviço aberta e ordem de
-- serviço atrasada. O NotificacaoConsumer os grava aqui.
--
-- O CHECK do `tipo` lista os valores aceitos e não acompanha o enum sozinho:
-- sem esta migração o banco recusa os tipos novos com SQLSTATE 23514, e a
-- mensagem é descartada pelo consumidor.
-- =============================================================================

ALTER TABLE notificacoes DROP CONSTRAINT IF EXISTS notificacoes_tipo_check;
ALTER TABLE notificacoes ADD CONSTRAINT notificacoes_tipo_check CHECK (tipo IN (
    'AVISO', 'ENTREGA', 'RESERVA', 'ALUGUEL_VAGA', 'PRE_AUTORIZACAO', 'CHAT', 'SISTEMA',
    'NOVA_FATURA', 'FATURA_VENCIDA', 'PAGAMENTO_CONFIRMADO', 'MULTA_APLICADA', 'MULTA_JULGADA',
    'OCORRENCIA_RESPONDIDA', 'ORDEM_SERVICO_ABERTA', 'ORDEM_SERVICO_ATRASADA'
));
