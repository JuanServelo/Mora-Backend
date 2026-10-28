package ocorrencias.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * O lado publicador da fila de notificações.
 *
 * Aqui só se declara a exchange: a fila e o vínculo são do consumidor
 * (comunicacao-service). Declarar a exchange dos dois lados é proposital — quem
 * subir primeiro a cria, e uma publicação não se perde por falta dela.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange notificacoesExchange(@Value("${notificacoes.exchange}") String nome) {
        return new TopicExchange(nome, true, false);
    }

    /** JSON, e não serialização Java: o consumidor é outro serviço. */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
