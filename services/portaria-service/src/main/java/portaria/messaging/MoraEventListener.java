package portaria.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import portaria.model.EventoRecebido;
import portaria.model.UserCache;
import portaria.repository.EventoRecebidoRepository;
import portaria.repository.UserCacheRepository;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Consumidor dos eventos de domínio publicados pelo Banco AUTH.
 *
 * Cada mensagem é registrada na inbox ({@link EventoRecebido}) para garantir
 * idempotência (reentregas do broker não são processadas duas vezes) e, em
 * seguida, despachada para o tratador correspondente ao tipo do evento.
 *
 * Falhas de processamento são logadas e a mensagem é confirmada (ack) para
 * evitar loops de "poison message"; o evento fica registrado como não processado
 * na inbox para reprocessamento manual/auditoria.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mora.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class MoraEventListener {

    private final EventoRecebidoRepository repository;
    private final UserCacheRepository userCacheRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    @Transactional
    public void onMoraEvent(MoraEvent event) {
        if (event == null || event.getEventId() == null) {
            log.warn("[mensageria] evento ignorado: envelope inválido ou sem eventId");
            return;
        }

        if (repository.existsById(event.getEventId())) {
            log.debug("[mensageria] evento {} já processado — ignorando reentrega", event.getEventId());
            return;
        }

        EventoRecebido registro = new EventoRecebido();
        registro.setEventId(event.getEventId());
        registro.setTipo(event.getType());
        registro.setPublicadoEm(event.getPublishedAt());
        registro.setPayload(serializarPayload(event.getPayload()));

        try {
            despachar(event);
            registro.setProcessado(true);
        } catch (Exception e) {
            registro.setProcessado(false);
            log.error("[mensageria] falha ao processar evento {} ({}): {}",
                    event.getType(), event.getEventId(), e.getMessage(), e);
        }

        repository.save(registro);
    }

    private void despachar(MoraEvent event) {
        String tipo = event.getType() == null ? "" : event.getType();
        Map<String, Object> p = event.getPayload();
        switch (tipo) {
            case "tenant.provisioned" -> log.info(
                    "[mensageria] tenant.provisioned recebido — schema='{}' name='{}' (preparar dados do tenant no Banco MORA)",
                    texto(p, "schemaName"), texto(p, "name"));
            case "tenant.suspended" -> log.info(
                    "[mensageria] tenant.suspended recebido — schema='{}' (desativar dados do tenant)",
                    texto(p, "schemaName"));
            case "user.created", "user.updated" -> sincronizarUsuario(tipo, p);
            case "user.deactivated" -> desativarUsuario(p);
            default -> log.warn("[mensageria] tipo de evento não tratado: {}", tipo);
        }
    }

    /** Insere ou atualiza o usuário no user_cache (eventos user.created / user.updated). */
    private void sincronizarUsuario(String tipo, Map<String, Object> p) {
        Long userId = numeroLong(p, "userId");
        if (userId == null) {
            log.warn("[mensageria] {} sem userId — ignorando", tipo);
            return;
        }

        UserCache cache = userCacheRepository.findById(userId).orElseGet(() -> {
            UserCache novo = new UserCache();
            novo.setUserId(userId);
            return novo;
        });

        if (p.containsKey("nome")) cache.setNome(texto(p, "nome"));
        if (p.containsKey("email")) cache.setEmail(texto(p, "email"));
        if (p.containsKey("perfil")) cache.setPerfil(texto(p, "perfil"));
        if (p.containsKey("condominioId")) cache.setCondominioId(texto(p, "condominioId"));
        if (p.containsKey("unidadeId")) cache.setUnidadeId(texto(p, "unidadeId"));
        if (p.containsKey("status")) cache.setActive("active".equalsIgnoreCase(texto(p, "status")));
        cache.setAtualizadoEm(LocalDateTime.now());

        userCacheRepository.save(cache);
        log.info("[mensageria] {} aplicado no user_cache — userId={} active={}", tipo, userId, cache.isActive());
    }

    /** Marca o usuário como inativo no user_cache (evento user.deactivated). */
    private void desativarUsuario(Map<String, Object> p) {
        Long userId = numeroLong(p, "userId");
        if (userId == null) {
            log.warn("[mensageria] user.deactivated sem userId — ignorando");
            return;
        }
        userCacheRepository.findById(userId).ifPresentOrElse(cache -> {
            cache.setActive(false);
            cache.setAtualizadoEm(LocalDateTime.now());
            userCacheRepository.save(cache);
            log.info("[mensageria] user.deactivated aplicado — userId={} is_active=false", userId);
        }, () -> log.info("[mensageria] user.deactivated para userId={} ausente no user_cache — nada a fazer", userId));
    }

    private String texto(Map<String, Object> payload, String chave) {
        Object v = payload == null ? null : payload.get(chave);
        return v == null ? null : String.valueOf(v);
    }

    private Long numeroLong(Map<String, Object> payload, String chave) {
        Object v = payload == null ? null : payload.get(chave);
        if (v instanceof Number n) return n.longValue();
        if (v == null) return null;
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String serializarPayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return String.valueOf(payload);
        }
    }
}
