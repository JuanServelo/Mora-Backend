package portaria.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import portaria.exception.OperacaoInvalidaException;
import portaria.security.AuthContext;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso somente-leitura ao auth-api.
 *
 * Quem mora em qual unidade vive no banco do auth-api, fora do alcance deste
 * serviço. Em vez de confiar num id de pessoa vindo no corpo da requisição,
 * repassamos o token do próprio chamador: o auth-api resolve a unidade dele e
 * devolve só os moradores dela, então a lista já chega no escopo certo.
 */
@Component
public class AuthApiClient {

    private static final Logger log = LoggerFactory.getLogger(AuthApiClient.class);

    private final String baseUrl;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    private final String tokenServico;

    public AuthApiClient(@Value("${auth.api.url}") String baseUrl,
                         @Value("${servico.token:}") String tokenServico) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.tokenServico = tokenServico == null ? "" : tokenServico.trim();
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public record PessoaUnidade(String id, String nome, String email, String perfil) {}

    /** Moradores ativos da unidade do usuário autenticado. */
    public List<PessoaUnidade> pessoasDaMinhaUnidade() {
        return buscar("/api/portaria/pessoas-unidade", "pessoas");
    }

    /**
     * Moradores ativos de uma unidade escolhida. O auth-api só honra o
     * parâmetro para porteiro e síndico; morador recebe sempre a própria.
     */
    public List<PessoaUnidade> pessoasDaUnidade(String unidadeId) {
        return buscar("/api/portaria/pessoas-unidade?unidadeId="
                + URLEncoder.encode(unidadeId, StandardCharsets.UTF_8), "pessoas");
    }

    /**
     * Síndicos ativos do condomínio, pela rota interna do auth-api.
     *
     * Usa o segredo compartilhado, e não o JWT de quem está na requisição: o
     * caso de uso é avisar o síndico de uma reserva pedida por um morador, e o
     * token do morador não autoriza listar a equipe — daria 403.
     *
     * Devolve lista vazia em qualquer falha: o aviso é desejável, mas não pode
     * impedir a reserva de ser gravada.
     */
    public List<PessoaUnidade> sindicos(String condominioId) {
        if (tokenServico.isEmpty()) {
            log.warn("[auth-api] SERVICO_TOKEN ausente — não dá para consultar síndicos");
            return List.of();
        }
        if (condominioId == null || condominioId.isBlank()) return List.of();

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/interno/sindicos?condominioId="
                            + URLEncoder.encode(condominioId, StandardCharsets.UTF_8)))
                    .header("Accept", "application/json")
                    .header("X-Servico-Token", tokenServico)
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 300) {
                log.warn("[auth-api] consulta de síndicos respondeu HTTP {}", res.statusCode());
                return List.of();
            }
            JsonNode raiz = mapper.readTree(res.body()).path("sindicos");
            List<PessoaUnidade> saida = new java.util.ArrayList<>();
            for (JsonNode n : raiz) {
                saida.add(new PessoaUnidade(
                        n.path("id").asText(), n.path("nome").asText(),
                        n.path("email").asText(), n.path("perfil").asText()));
            }
            return saida;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (Exception e) {
            log.warn("[auth-api] não foi possível consultar síndicos: {}", e.getMessage());
            return List.of();
        }
    }

    /** Funcionários elegíveis como responsáveis por evento do condomínio. */
    public List<PessoaUnidade> funcionarios() {
        return buscar("/api/portaria/funcionarios", "funcionarios");
    }

    private List<PessoaUnidade> buscar(String caminho, String campo) {
        String token = AuthContext.getToken();
        if (token == null) {
            throw new OperacaoInvalidaException("Usuário não autenticado");
        }

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + caminho))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> res;
        try {
            res = http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OperacaoInvalidaException("Consulta de moradores interrompida.");
        } catch (Exception e) {
            // Falha aberta seria pior que falha fechada: sem a lista não há como
            // validar a unidade, e aceitar o vínculo criaria dado inconsistente.
            throw new OperacaoInvalidaException(
                    "Não foi possível consultar os moradores da unidade. Tente novamente em instantes.");
        }

        if (res.statusCode() != 200) {
            throw new OperacaoInvalidaException(
                    "Não foi possível consultar os moradores da unidade (HTTP " + res.statusCode() + ").");
        }

        try {
            JsonNode raiz = mapper.readTree(res.body());
            JsonNode lista = raiz.path(campo);
            List<PessoaUnidade> pessoas = new ArrayList<>();
            for (JsonNode p : lista) {
                pessoas.add(new PessoaUnidade(
                        p.path("id").asText(null),
                        p.path("nome").asText(null),
                        p.path("email").asText(null),
                        p.path("perfil").asText(null)));
            }
            return pessoas;
        } catch (Exception e) {
            throw new OperacaoInvalidaException("Resposta inesperada ao consultar os moradores da unidade.");
        }
    }
}
