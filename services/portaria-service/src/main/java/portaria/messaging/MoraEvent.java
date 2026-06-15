package portaria.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/**
 * Envelope de um evento de domínio recebido do Banco AUTH (auth-api).
 *
 * Formato publicado em utils/eventPublisher.js do auth-api:
 * <pre>
 * { "eventId": "...", "type": "tenant.provisioned", "payload": { ... }, "publishedAt": "ISO" }
 * </pre>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MoraEvent {

    private String eventId;
    private String type;
    private Map<String, Object> payload;
    private String publishedAt;
}
