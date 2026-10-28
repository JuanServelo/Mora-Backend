package ocorrencias.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Nome e e-mail de quem abriu cada ocorrência, segundo o auth-api.
 *
 * Quando a reclamação vivia no auth-api, a lista da gestão fazia um JOIN com
 * `users`. Agora os bancos são separados: a gestão pede a lista de usuários do
 * condomínio, em nome de quem está pedindo (repassando o Bearer).
 *
 * **Falha macia, de propósito.** Sem o auth-api a lista aparece sem o nome do
 * morador, o que é bem melhor que a lista inteira não carregar.
 */
@Component
public class AuthClient {

    private static final Logger log = LoggerFactory.getLogger(AuthClient.class);

    private final RestClient rest;

    public AuthClient(@Value("${auth.api.url:http://auth-api:3001}") String baseUrl) {
        this.rest = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(fabricaComTimeout())
                .build();
    }

    /** Uma fonte lenta não pode segurar a tela. */
    private static org.springframework.http.client.ClientHttpRequestFactory fabricaComTimeout() {
        var settings = org.springframework.boot.web.client.ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(2))
                .withReadTimeout(Duration.ofSeconds(3));
        return org.springframework.boot.web.client.ClientHttpRequestFactories.get(settings);
    }

    /**
     * Usuários por id. Vazio quando o auth-api não responde.
     *
     * `condominioId` nulo é o Admin Geral olhando todos os condomínios.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Usuario> usuariosPorId(String condominioId, String authorization) {
        if (authorization == null || authorization.isBlank()) return Map.of();

        try {
            Map<String, Object> corpo = rest.get()
                    .uri(uri -> {
                        uri.path("/api/user-management/users");
                        if (condominioId != null) uri.queryParam("condominioId", condominioId);
                        return uri.build();
                    })
                    .header("Authorization", authorization)
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> usuarios =
                    (List<Map<String, Object>>) (corpo == null ? null : corpo.get("usuarios"));
            if (usuarios == null) return Map.of();

            Map<String, Usuario> porId = new HashMap<>();
            for (Map<String, Object> u : usuarios) {
                Object id = u.get("id");
                if (id == null) continue;
                String email = (String) u.get("email");
                Object nome = u.get("nome");
                porId.put(String.valueOf(id), new Usuario(nome == null ? email : String.valueOf(nome), email));
            }
            return porId;
        } catch (Exception e) {
            log.warn("auth-api não respondeu a listagem de usuários: {}", e.getMessage());
            return Map.of();
        }
    }

    public record Usuario(String nome, String email) {}
}
