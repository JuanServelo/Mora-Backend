package comunicacao.controller;

import comunicacao.exception.OperacaoInvalidaException;
import comunicacao.model.ChatMensagem;
import comunicacao.security.AuthContext;
import comunicacao.security.JwtClaims;
import comunicacao.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Tag(name = "Chat direto", description = "Mensagens diretas entre dois usuários do mesmo condomínio. Para falar com a administração, use as Conversas.")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/mensagem")
    @Operation(summary = "Enviar mensagem", description = """
            Envia uma mensagem direta para `destinatarioId`. O corpo é o texto puro, não JSON. O destinatário recebe uma \
            notificação. Qualquer perfil com acesso ao sistema.""")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Texto da mensagem, como texto puro.", required = true,
            content = {@Content(mediaType = "text/plain", examples = @ExampleObject(value = "Oi, Ana. A encomenda que chegou ontem já está na portaria.")),
                @Content(mediaType = "application/json", schema = @Schema(type = "string"), examples = @ExampleObject(value = "\"Oi, Ana. A encomenda que chegou ontem já está na portaria.\""))})
    @ApiResponse(responseCode = "201", description = "Mensagem enviada.")
    public ResponseEntity<ChatMensagem> enviar(
            @Parameter(description = "Id de quem recebe (auth-api).", example = "4") @RequestParam String destinatarioId,
            @RequestBody String texto) {
        ChatMensagem msg = chatService.enviar(currentUserId(), destinatarioId, texto);
        return ResponseEntity.status(HttpStatus.CREATED).body(msg);
    }

    @GetMapping("/conversa/{outroUsuarioId}")
    @Operation(summary = "Conversa com um usuário", description = "Todas as mensagens trocadas com `outroUsuarioId`, em ordem.")
    @ApiResponse(responseCode = "200", description = "Mensagens.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            [
              {
                "id": "8e1d4c7a-2b9f-4a3e-b5d6-0c7f1e2a9b84",
                "remetenteId": "2",
                "destinatarioId": "4",
                "condominioId": "cond-parque-verde",
                "texto": "Oi, Ana. A encomenda que chegou ontem já está na portaria.",
                "lida": false,
                "enviadoEm": "2026-10-20T10:42:00",
                "lidaEm": null
              }
            ]""")))
    public List<ChatMensagem> conversa(@Parameter(description = "Id do outro usuário (auth-api).", example = "4") @PathVariable String outroUsuarioId) {
        return chatService.buscarConversa(currentUserId(), outroUsuarioId);
    }

    @PatchMapping("/conversa/{outroUsuarioId}/lida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marcar conversa como lida", description = "Marca como lidas todas as mensagens recebidas de `outroUsuarioId`.")
    @ApiResponse(responseCode = "204", description = "Marcadas.")
    public void marcarConversaLida(@Parameter(description = "Id do outro usuário (auth-api).", example = "4") @PathVariable String outroUsuarioId) {
        chatService.marcarConversaLida(currentUserId(), outroUsuarioId);
    }

    @GetMapping("/nao-lidas")
    @Operation(summary = "Mensagens não lidas", description = "Mensagens recebidas que quem chama ainda não leu.")
    @ApiResponse(responseCode = "200", description = "Mensagens.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            [
              {
                "id": "8e1d4c7a-2b9f-4a3e-b5d6-0c7f1e2a9b84",
                "remetenteId": "2",
                "destinatarioId": "4",
                "condominioId": "cond-parque-verde",
                "texto": "Oi, Ana. A encomenda que chegou ontem já está na portaria.",
                "lida": false,
                "enviadoEm": "2026-10-20T10:42:00",
                "lidaEm": null
              }
            ]""")))
    public List<ChatMensagem> listarNaoLidas() {
        return chatService.listarNaoLidas(currentUserId());
    }

    @GetMapping("/contador")
    @Operation(summary = "Contador de não lidas", description = "Quantas mensagens diretas quem chama ainda não leu.")
    @ApiResponse(responseCode = "200", description = "Contagem.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            { "naoLidas": 2 }""")))
    public Map<String, Long> contador() {
        return Map.of("naoLidas", chatService.contarNaoLidas(currentUserId()));
    }

    @PatchMapping("/mensagem/{id}/lida")
    @Operation(summary = "Marcar mensagem como lida", description = "Só o destinatário marca a própria mensagem.")
    @ApiResponse(responseCode = "200", description = "Mensagem marcada.")
    public ChatMensagem marcarLida(@Parameter(description = "Id da mensagem.", example = "8e1d4c7a-2b9f-4a3e-b5d6-0c7f1e2a9b84") @PathVariable String id) {
        return chatService.marcarLida(id, currentUserId());
    }

    /**
     * O id de quem está na requisição, como o token o traz.
     *
     * Sem conversão: o `auth-api` numera usuários com `integer`, e
     * `UUID.fromString("32")` derrubava esta rota com 500.
     */
    private String currentUserId() {
        JwtClaims claims = AuthContext.get();
        if (claims == null || claims.authUserId() == null) {
            throw new OperacaoInvalidaException("Usuário não autenticado");
        }
        return claims.authUserId();
    }
}
