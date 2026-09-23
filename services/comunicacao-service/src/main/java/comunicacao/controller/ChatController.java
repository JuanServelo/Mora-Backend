package comunicacao.controller;

import comunicacao.dto.ConversaResponse;
import comunicacao.dto.MensagemRequest;
import comunicacao.dto.MensagemResponse;
import comunicacao.dto.UsuarioResumo;
import comunicacao.security.PerfilUtils;
import comunicacao.service.ChatService;
import jakarta.validation.Valid;
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
    public ResponseEntity<MensagemResponse> enviar(@Valid @RequestBody MensagemRequest req) {
        MensagemResponse msg = chatService.enviar(
                PerfilUtils.usuarioAtual(), req.destinatarioId(), req.texto());
        return ResponseEntity.status(HttpStatus.CREATED).body(msg);
    }

    /** Caixa de entrada: uma linha por interlocutor. */
    @GetMapping("/conversas")
    public List<ConversaResponse> conversas() {
        return chatService.listarConversas(PerfilUtils.usuarioAtual());
    }

    /** Com quem este usuario pode iniciar uma conversa. */
    @GetMapping("/contatos")
    public List<UsuarioResumo> contatos() {
        return chatService.contatos();
    }

    @GetMapping("/conversa/{outroUsuarioId}")
    public List<MensagemResponse> conversa(@PathVariable String outroUsuarioId) {
        return chatService.buscarConversa(PerfilUtils.usuarioAtual(), outroUsuarioId);
    }

    @PatchMapping("/conversa/{outroUsuarioId}/lida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarConversaLida(@PathVariable String outroUsuarioId) {
        chatService.marcarConversaLida(PerfilUtils.usuarioAtual(), outroUsuarioId);
    }

    @PatchMapping("/mensagem/{id}/lida")
    public MensagemResponse marcarLida(@PathVariable String id) {
        return chatService.marcarLida(id, PerfilUtils.usuarioAtual());
    }

    @GetMapping("/contador")
    public Map<String, Long> contador() {
        return Map.of("naoLidas", chatService.contarNaoLidas(PerfilUtils.usuarioAtual()));
    }
}
