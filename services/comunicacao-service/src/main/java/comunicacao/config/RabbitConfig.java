package comunicacao.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A fila de notificações vista do lado de quem consome.
 *
 * Os serviços publicam na exchange `mora.notificacoes` com a chave
 * `notificacao.<origem>`; este serviço é dono da fila e a liga a tudo que
 * começa com `notificacao.`. Durável dos dois lados: uma notificação publicada
 * enquanto o comunicacao está parado espera na fila até ele voltar.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange notificacoesExchange(@Value("${notificacoes.exchange}") String nome) {
        return new TopicExchange(nome, true, false);
    }

    @Bean
    public Queue notificacoesFila(@Value("${notificacoes.fila}") String nome) {
        return new Queue(nome, true);
    }

    @Bean
    public Binding notificacoesBinding(Queue notificacoesFila, TopicExchange notificacoesExchange) {
        return BindingBuilder.bind(notificacoesFila).to(notificacoesExchange).with("notificacao.#");
    }

    /** JSON: quem publica é outro serviço, não há classe Java em comum. */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
