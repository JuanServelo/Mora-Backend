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

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/mensagem")
    public ResponseEntity<ChatMensagem> enviar(
            @RequestParam String destinatarioId,
            @RequestBody String texto) {
        ChatMensagem msg = chatService.enviar(currentUserId(), destinatarioId, texto);
        return ResponseEntity.status(HttpStatus.CREATED).body(msg);
    }

    @GetMapping("/conversa/{outroUsuarioId}")
    public List<ChatMensagem> conversa(@PathVariable String outroUsuarioId) {
        return chatService.buscarConversa(currentUserId(), outroUsuarioId);
    }

    @PatchMapping("/conversa/{outroUsuarioId}/lida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarConversaLida(@PathVariable String outroUsuarioId) {
        chatService.marcarConversaLida(currentUserId(), outroUsuarioId);
    }

    @GetMapping("/nao-lidas")
    public List<ChatMensagem> listarNaoLidas() {
        return chatService.listarNaoLidas(currentUserId());
    }

    @GetMapping("/contador")
    public Map<String, Long> contador() {
        return Map.of("naoLidas", chatService.contarNaoLidas(currentUserId()));
    }

    @PatchMapping("/mensagem/{id}/lida")
    public ChatMensagem marcarLida(@PathVariable String id) {
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
