package comunicacao.controller;

import comunicacao.model.Notificacao;
import comunicacao.service.NotificacaoService;
import comunicacao.service.NotificacaoService.Publicacao;
import comunicacao.service.NotificacaoService.Resultado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Por onde outros serviços publicam notificação para um usuário.
 *
 * O caso que motiva a rota é o fechamento do mês no financeiro: ele roda como
 * **job, sem usuário logado**, então não há JWT para repassar — e um JWT
 * fabricado não passaria, porque o auth-api confere o usuário no banco. A
 * credencial aqui é um segredo compartilhado entre serviços, `SERVICO_TOKEN`.
 *
 * Fica fora do `AuthFilter`; a autenticação é feita aqui.
 */
@RestController
@RequestMapping("/interno/notificacoes")
@Tag(name = "Entre serviços", description = "Rota que outros serviços usam para publicar notificações. Autenticada por X-Servico-Token, não por JWT.")
public class NotificacaoInternaController {

    private final NotificacaoService notificacaoService;
    private final byte[] tokenServico;

    public NotificacaoInternaController(NotificacaoService notificacaoService,
                                        @Value("${servico.token:}") String tokenServico) {
        this.notificacaoService = notificacaoService;
        this.tokenServico = tokenServico == null ? new byte[0] : tokenServico.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping
    @Operation(summary = "Publicar notificação (entre serviços)", description = """
            Usada pelo financeiro ao aplicar multa ou fechar a competência: um job, sem usuário logado, por isso a \
            credencial é o segredo compartilhado `SERVICO_TOKEN`, no cabeçalho `X-Servico-Token`. Sem token configurado no \
            serviço, responde 503 (a rota fica fechada, não aberta). Reenvio com a mesma `chaveUnica` responde 200 com \
            `repetida: true`, sem duplicar.""")
    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @Parameter(name = "X-Servico-Token", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER, description = "Segredo compartilhado entre serviços (SERVICO_TOKEN).", example = "********")
    @ApiResponse(responseCode = "201", description = "Notificação criada.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            { "sucesso": true, "repetida": false, "notificacao": { "id": "5b9e2f1c-7d4a-4c8e-a3b6-9f0d1e2c7a45", "destinatarioId": "4", "condominioId": "cond-parque-verde", "tipo": "MULTA_APLICADA",
                  "titulo": "Multa aplicada à sua unidade", "mensagem": "Barulho após as 22h em dia de semana — R$ 150,00. Você pode recorrer até 28/10.",
                  "referenciaId": "9c1f4e2a-6b7d-4f3a-a2c8-51e0d7b3f6a9", "lida": false, "criadoEm": "2026-10-13T14:20:05", "lidaEm": null,
                  "origem": "financeiro", "chaveUnica": "financeiro:MULTA_APLICADA:9c1f4e2a-6b7d-4f3a-a2c8-51e0d7b3f6a9",
                  "dados": { "multaId": "9c1f4e2a-6b7d-4f3a-a2c8-51e0d7b3f6a9" } } }""")))
    @ApiResponse(responseCode = "200", description = "Já tinha sido publicada com esta chave: nada foi duplicado.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            { "sucesso": true, "repetida": true, "notificacao": { "id": "5b9e2f1c-7d4a-4c8e-a3b6-9f0d1e2c7a45", "destinatarioId": "4", "condominioId": "cond-parque-verde", "tipo": "MULTA_APLICADA",
                  "titulo": "Multa aplicada à sua unidade", "mensagem": "Barulho após as 22h em dia de semana — R$ 150,00. Você pode recorrer até 28/10.",
                  "referenciaId": "9c1f4e2a-6b7d-4f3a-a2c8-51e0d7b3f6a9", "lida": false, "criadoEm": "2026-10-13T14:20:05", "lidaEm": null,
                  "origem": "financeiro", "chaveUnica": "financeiro:MULTA_APLICADA:9c1f4e2a-6b7d-4f3a-a2c8-51e0d7b3f6a9",
                  "dados": { "multaId": "9c1f4e2a-6b7d-4f3a-a2c8-51e0d7b3f6a9" } } }""")))
    @ApiResponse(responseCode = "401", description = "Token de serviço ausente ou errado.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            { "sucesso": false, "mensagem": "Token de serviço inválido." }""")))
    @ApiResponse(responseCode = "503", description = "SERVICO_TOKEN não configurado no comunicacao.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            { "sucesso": false, "mensagem": "Publicação entre serviços não configurada." }""")))
    public ResponseEntity<Map<String, Object>> publicar(
            @RequestHeader(value = "X-Servico-Token", required = false) String token,
            @RequestBody Publicacao publicacao) {

        // Sem token configurado a rota fica FECHADA, e não aberta. Uma variável
        // esquecida no deploy não pode virar uma porta para qualquer um escrever
        // na caixa de entrada de qualquer usuário.
        if (tokenServico.length == 0) {
            return resposta(HttpStatus.SERVICE_UNAVAILABLE,
                    Map.of("sucesso", false, "mensagem", "Publicação entre serviços não configurada."));
        }
        if (!tokenConfere(token)) {
            return resposta(HttpStatus.UNAUTHORIZED,
                    Map.of("sucesso", false, "mensagem", "Token de serviço inválido."));
        }

        Resultado r;
        try {
            r = notificacaoService.publicarDeServico(publicacao);
        } catch (DataIntegrityViolationException e) {
            // Duas publicações simultâneas com a mesma chave: a segunda esbarrou
            // no índice único. Para quem publicou, é uma repetição como outra.
            Notificacao existente = notificacaoService.buscarPorChave(publicacao.chaveUnica());
            if (existente == null) throw e;
            r = new Resultado(existente, true);
        }

        Map<String, Object> corpo = new HashMap<>();
        corpo.put("sucesso", true);
        corpo.put("repetida", r.repetida());
        corpo.put("notificacao", r.notificacao());
        return resposta(r.repetida() ? HttpStatus.OK : HttpStatus.CREATED, corpo);
    }

    /**
     * Comparação em tempo constante.
     *
     * `equals` para no primeiro caractere diferente, e o tempo da resposta
     * entrega quantos acertaram — dá para descobrir o token por tentativa,
     * caractere a caractere.
     */
    boolean tokenConfere(String recebido) {
        if (recebido == null) return false;
        return MessageDigest.isEqual(tokenServico, recebido.getBytes(StandardCharsets.UTF_8));
    }

    private static ResponseEntity<Map<String, Object>> resposta(HttpStatus status, Map<String, Object> corpo) {
        return ResponseEntity.status(status).body(corpo);
    }
}
