package portaria.client;

import com.fasterxml.jackson.databind.JsonNode;
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

    private final String baseUrl;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public AuthApiClient(@Value("${auth.api.url}") String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
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
