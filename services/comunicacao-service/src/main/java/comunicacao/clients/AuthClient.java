package comunicacao.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Quem são os usuários de um condomínio, segundo o auth-api.
 *
 * Serve a dois usos: a gestão escolher o destinatário de uma conversa direta, e
 * a conversa mostrar nome e foto da contraparte — "Infiltração na garagem" não
 * diz de quem é, e uma fila de chamados sem nome obriga a abrir um por um.
 *
 * **Devolve o condomínio inteiro só para a gestão.** O auth-api aceita a rota
 * para morador, mas recorta o resultado na unidade dele: ele pede a lista e
 * recebe a si mesmo e os ocupantes do próprio apartamento. É exatamente por
 * isso que a conversa do morador é endereçada à *administração* e não a uma
 * pessoa — o síndico nunca aparece na lista que ele alcança.
 *
 * **Falha macia, de propósito.** Sem o auth-api a conversa aparece sem o nome
 * da contraparte, o que é bem melhor que a lista inteira não carregar.
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

    /**
     * Uma fonte lenta não pode segurar a tela.
     *
     * Sem timeout, o auth-api indisponível deixaria a listagem de conversas
     * pendurada até o timeout do servidor — e o usuário vendo um spinner que
     * não termina.
     */
    private static org.springframework.http.client.ClientHttpRequestFactory fabricaComTimeout() {
        var settings = org.springframework.boot.web.client.ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(2))
                .withReadTimeout(Duration.ofSeconds(3));
        return org.springframework.boot.web.client.ClientHttpRequestFactories.get(settings);
    }

    /** `null` quando o auth-api não responde — diferente de lista vazia. */
    @SuppressWarnings("unchecked")
    public List<Usuario> listarUsuariosDoCondominio(String condominioId, String authorization) {
        if (authorization == null || authorization.isBlank()) return null;

        try {
            Map<String, Object> corpo = rest.get()
                    .uri(uri -> uri.path("/api/user-management/users")
                            .queryParam("condominioId", condominioId)
                            .build())
                    .header("Authorization", authorization)
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> usuarios =
                    (List<Map<String, Object>>) (corpo == null ? null : corpo.get("usuarios"));
            if (usuarios == null) return List.of();

            return usuarios.stream().map(AuthClient::converter).toList();
        } catch (Exception e) {
            log.warn("auth-api não respondeu a listagem de usuários: {}", e.getMessage());
            return null;
        }
    }

    private static Usuario converter(Map<String, Object> u) {
        Object id = u.get("id");
        String status = (String) u.get("status");
        boolean semAcesso = Boolean.TRUE.equals(u.get("semAcessoSistema"));

        return new Usuario(
                id == null ? null : String.valueOf(id),
                (String) u.getOrDefault("nome", u.get("email")),
                (String) u.get("email"),
                (String) u.get("perfil"),
                (String) u.get("fotoUrl"),
                u.get("unidadeId") == null ? null : String.valueOf(u.get("unidadeId")),
                // Quem não acessa o sistema — o morador cadastrado só para
                // liberar entrada — não entra em lista de destinatário.
                "active".equals(status) && !semAcesso
        );
    }

    public record Usuario(
            String id,
            String nome,
            String email,
            String perfil,
            String fotoUrl,
            String unidadeId,
            boolean alcancavel
    ) {}
}
