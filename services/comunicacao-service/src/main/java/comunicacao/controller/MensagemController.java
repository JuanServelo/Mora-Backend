package comunicacao.controller;

import comunicacao.service.ConversaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Remoção de mensagem.
 *
 * Fora do `ConversaController` porque o caminho é `/mensagens/{id}`: a mensagem
 * é encontrada pelo próprio id, sem precisar da conversa. Aninhar em
 * `/conversas/{id}/mensagens/{msgId}` obrigaria quem chama a carregar um dado
 * que ele não precisa ter à mão.
 */
@RestController
@RequestMapping("/mensagens")
@RequiredArgsConstructor
public class MensagemController {

    private final ConversaService conversaService;

    @DeleteMapping("/{id}")
    public Map<String, Object> remover(@PathVariable Long id) {
        return conversaService.removerMensagem(id);
    }
}
