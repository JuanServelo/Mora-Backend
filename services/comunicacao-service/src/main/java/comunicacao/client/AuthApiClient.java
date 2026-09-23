package comunicacao.client;

import comunicacao.dto.UsuarioResumo;
import comunicacao.security.AuthContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Acesso ao auth-api, que é o dono da identidade dos usuários.
 *
 * Este serviço guarda apenas o id de quem escreveu ou deve receber algo; nome,
 * perfil e unidade vêm daqui. As chamadas repassam o Bearer de quem originou a
 * requisição, então o auth-api aplica as mesmas regras de escopo por condomínio
 * que aplicaria se o frontend tivesse chamado direto.
 */
@Component
public class AuthApiClient {

    private static final Logger log = LoggerFactory.getLogger(AuthApiClient.class);

    private final RestClient rest;

    public AuthApiClient(@Value("${auth-api.url:http://localhost:3001}") String baseUrl) {
        this.rest = RestClient.builder().baseUrl(baseUrl).build();
    }

    /** Com quem o usuário atual pode conversar. */
    public List<UsuarioResumo> contatos() {
        ContatosResponse res = get("/api/users/contatos", ContatosResponse.class);
        return res == null || res.contatos() == null ? List.of() : res.contatos();
    }

    /** Destinatários de um aviso — base do denominador do percentual de leitura. */
    public List<UsuarioResumo> destinatarios(String publicoAlvo) {
        DestinatariosResponse res = get(
                "/api/users/destinatarios?publicoAlvo=" + (publicoAlvo == null ? "TODOS" : publicoAlvo),
                DestinatariosResponse.class);
        return res == null || res.destinatarios() == null ? List.of() : res.destinatarios();
    }

    /**
     * Resolve vários ids de uma vez.
     *
     * Devolve um mapa para que a montagem de listas (leitores, conversas) não
     * precise de uma chamada por participante. Ids que o auth-api não devolveu
     * viram um resumo genérico em vez de sumirem da lista.
     */
    public Map<String, UsuarioResumo> buscarPorIds(Collection<String> ids) {
        List<String> distintos = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distintos.isEmpty()) return Map.of();

        Map<String, UsuarioResumo> porId = new LinkedHashMap<>();
        try {
            LookupResponse res = rest.post()
                    .uri("/api/users/lookup")
                    .header("Authorization", bearer())
                    .body(Map.of("ids", distintos))
                    .retrieve()
                    .body(LookupResponse.class);
            if (res != null && res.usuarios() != null) {
                res.usuarios().forEach(u -> porId.putIfAbsent(u.id(), u));
            }
        } catch (Exception e) {
            log.warn("Falha ao resolver usuários no auth-api: {}", e.getMessage());
        }
        distintos.forEach(id -> porId.putIfAbsent(id, UsuarioResumo.desconhecido(id)));
        return porId;
    }

    public UsuarioResumo buscarPorId(String id) {
        return buscarPorIds(List.of(id)).get(id);
    }

    /**
     * Uma falha do auth-api não pode derrubar a tela inteira: quem chama segue
     * com a lista vazia e exibe o que tiver.
     */
    private <T> T get(String uri, Class<T> tipo) {
        try {
            return rest.get()
                    .uri(uri)
                    .header("Authorization", bearer())
                    .retrieve()
                    .body(tipo);
        } catch (Exception e) {
            log.warn("Falha ao consultar {} no auth-api: {}", uri, e.getMessage());
            return null;
        }
    }

    private String bearer() {
        String token = AuthContext.token();
        if (token == null) throw new IllegalStateException("Sem token para repassar ao auth-api");
        return "Bearer " + token;
    }

    private record ContatosResponse(boolean sucesso, List<UsuarioResumo> contatos) {}
    private record DestinatariosResponse(boolean sucesso, List<UsuarioResumo> destinatarios) {}
    private record LookupResponse(boolean sucesso, List<UsuarioResumo> usuarios) {}
}
