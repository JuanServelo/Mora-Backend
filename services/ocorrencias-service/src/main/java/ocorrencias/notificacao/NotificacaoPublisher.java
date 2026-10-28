package ocorrencias.notificacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica notificações na fila, para o comunicacao-service entregar.
 *
 * **Só depois do commit.** Quem chama `publicar` está no meio de uma transação;
 * a mensagem fica guardada como evento e sai quando ela confirma. Se a
 * transação desfizer, a notificação não sai — o morador não pode ser avisado
 * de uma resposta que não foi gravada.
 *
 * **Falha macia.** Broker fora do ar não desfaz o atendimento: a resposta já
 * está gravada e o morador a vê na tela. Só o aviso deixa de sair, e o log diz.
 */
@Component
public class NotificacaoPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoPublisher.class);

    private final ApplicationEventPublisher eventos;
    private final RabbitTemplate rabbit;
    private final String exchange;
    private final String routingKey;

    public NotificacaoPublisher(ApplicationEventPublisher eventos,
                                RabbitTemplate rabbit,
                                @Value("${notificacoes.exchange}") String exchange,
                                @Value("${notificacoes.routing-key}") String routingKey) {
        this.eventos = eventos;
        this.rabbit = rabbit;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void publicar(NotificacaoEvento evento) {
        eventos.publishEvent(evento);
    }

    /** `fallbackExecution`: fora de transação, envia na hora. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void enviar(NotificacaoEvento evento) {
        try {
            rabbit.convertAndSend(exchange, routingKey, evento);
        } catch (AmqpException e) {
            log.warn("notificação {} não publicada (broker indisponível): {}", evento.chaveUnica(), e.getMessage());
        }
    }
}
