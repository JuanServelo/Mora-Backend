package portaria.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publica notificação para um morador, pela rota interna do comunicacao.
 *
 * A credencial é o segredo compartilhado `SERVICO_TOKEN`, e não o JWT de quem
 * está na portaria: o porteiro registra a entrega, mas quem recebe o aviso é o
 * morador, e o token do porteiro não autoriza escrever na caixa de outro.
 *
 * Falha aberta, de propósito: notificação é aviso, não registro. Se o
 * comunicacao estiver fora do ar ou o token não estiver configurado, a entrega
 * é gravada do mesmo jeito e o problema vai para o log. Perder o aviso é ruim;
 * impedir o porteiro de registrar a encomenda que está na mão dele é pior.
 */
@Component
public class ComunicacaoClient {

    private static final Logger log = LoggerFactory.getLogger(ComunicacaoClient.class);

    private final String baseUrl;
    private final String tokenServico;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public ComunicacaoClient(
            @Value("${comunicacao.service.url:http://comunicacao-service:8094}") String baseUrl,
            @Value("${servico.token:}") String tokenServico) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.tokenServico = tokenServico == null ? "" : tokenServico.trim();
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    /**
     * @param chaveUnica identifica a publicação: reenviar a mesma chave não
     *                   duplica a notificação, o comunicacao responde 200 com
     *                   {@code repetida: true}. É o que torna seguro chamar
     *                   daqui sem transação distribuída.
     */
    public void notificar(String destinatarioId, String condominioId, String tipo,
                          String titulo, String mensagem, String chaveUnica,
                          Map<String, Object> dados) {
        if (tokenServico.isEmpty()) {
            log.warn("[comunicacao] SERVICO_TOKEN ausente — notificação '{}' não publicada", chaveUnica);
            return;
        }
        if (destinatarioId == null || destinatarioId.isBlank()) {
            log.debug("[comunicacao] sem destinatário para '{}' — nada a publicar", chaveUnica);
            return;
        }

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("condominioId", condominioId);
        corpo.put("usuarioId", destinatarioId);
        corpo.put("origem", "portaria");
        corpo.put("tipo", tipo);
        corpo.put("titulo", titulo);
        corpo.put("mensagem", mensagem);
        corpo.put("chaveUnica", chaveUnica);
        corpo.put("dados", dados == null ? Map.of() : dados);

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/interno/notificacoes"))
                    .header("Content-Type", "application/json")
                    .header("X-Servico-Token", tokenServico)
                    .timeout(Duration.ofSeconds(3))
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(corpo)))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 300) {
                log.warn("[comunicacao] publicação '{}' respondeu HTTP {}", chaveUnica, res.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[comunicacao] publicação '{}' interrompida", chaveUnica);
        } catch (Exception e) {
            log.warn("[comunicacao] não foi possível publicar '{}': {}", chaveUnica, e.getMessage());
        }
    }
}
