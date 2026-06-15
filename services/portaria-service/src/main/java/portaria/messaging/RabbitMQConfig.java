package portaria.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia de mensageria do Banco MORA (lado consumidor).
 *
 * Liga uma fila durável (`portaria.mora-events`) ao topic exchange `mora.events`
 * publicado pelo auth-api, assinando os tópicos `tenant.*` e `user.*`
 * (ver docs/assincrona.md). A declaração é idempotente: o exchange já é criado
 * pelo publicador, e a fila/bindings são criados por este serviço.
 */
@Configuration
@ConditionalOnProperty(name = "mora.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {

    public static final String EXCHANGE = "mora.events";
    public static final String QUEUE = "portaria.mora-events";

    @Bean
    public TopicExchange moraEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue portariaMoraEventsQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding bindTenantEvents(Queue portariaMoraEventsQueue, TopicExchange moraEventsExchange) {
        return BindingBuilder.bind(portariaMoraEventsQueue).to(moraEventsExchange).with("tenant.*");
    }

    @Bean
    public Binding bindUserEvents(Queue portariaMoraEventsQueue, TopicExchange moraEventsExchange) {
        return BindingBuilder.bind(portariaMoraEventsQueue).to(moraEventsExchange).with("user.*");
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
