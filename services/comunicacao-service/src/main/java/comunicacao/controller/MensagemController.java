package comunicacao.controller;

import comunicacao.service.ConversaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "Conversas", description = "Conversas com assunto: o morador falando com a administração (ADMINISTRACAO) ou a gestão com uma pessoa (DIRETA).")
public class MensagemController {

    private final ConversaService conversaService;

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover mensagem", description = """
            Remove uma mensagem que quem chama escreveu. A mensagem continua na conversa, sem o texto, marcada com \
            `removidaEm`: assim a ordem da conversa não muda.""")
    @Parameter(name = "id", description = "Id da mensagem.", example = "88")
    @ApiResponse(responseCode = "200", description = "Mensagem removida.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            { "sucesso": true, "mensagem": { "id": 88, "conversaId": 12, "autorId": "2", "autorPerfil": "ADMIN_SINDICO",
              "corpo": "Obrigado, Ana. Vou pedir ao zelador para olhar o 201 amanhã cedo.", "removidaEm": "2026-10-20T16:30:00", "criadaEm": "2026-10-20T16:02:00" } }""")))
    public Map<String, Object> remover(@PathVariable Long id) {
        return conversaService.removerMensagem(id);
    }
}
