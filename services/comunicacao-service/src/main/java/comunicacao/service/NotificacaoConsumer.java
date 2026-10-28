package comunicacao.service;

import comunicacao.exception.DadosInvalidosException;
import comunicacao.service.NotificacaoService.Publicacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Notificações que chegam pela fila — hoje, as do ocorrencias-service.
 *
 * É a mesma publicação da rota `/interno/notificacoes`, por outro transporte:
 * mesma validação e mesma deduplicação por `chaveUnica`. A diferença é que
 * quem publica não espera — se este serviço estiver parado, a mensagem aguarda
 * na fila.
 */
@Component
public class NotificacaoConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoConsumer.class);

    private final NotificacaoService notificacaoService;

    public NotificacaoConsumer(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @RabbitListener(queues = "${notificacoes.fila}")
    public void receber(Publicacao publicacao) {
        try {
            var r = notificacaoService.publicarDeServico(publicacao);
            if (r.repetida()) log.debug("notificação repetida ignorada: {}", publicacao.chaveUnica());
        } catch (DataIntegrityViolationException e) {
            // Duas entregas simultâneas da mesma chave: a segunda esbarrou no
            // índice único. Já está gravada — nada a fazer.
            log.debug("notificação repetida ignorada: {}", publicacao.chaveUnica());
        } catch (DadosInvalidosException e) {
            // Tentar de novo não conserta uma mensagem malformada: descarta.
            log.warn("notificação recusada ({}): {}", publicacao.origem(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
        }
    }
}
