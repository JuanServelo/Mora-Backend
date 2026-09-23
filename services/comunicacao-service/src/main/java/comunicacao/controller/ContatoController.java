package comunicacao.controller;

import comunicacao.service.ConversaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Com quem a gestão pode abrir uma conversa direta.
 *
 * Só a gestão: a listagem que o morador alcança no auth-api é recortada na
 * unidade dele, então para ele esta rota não teria o que oferecer — e é
 * justamente por isso que a conversa dele é endereçada à administração.
 */
@RestController
@RequestMapping("/contatos")
@RequiredArgsConstructor
public class ContatoController {

    private final ConversaService conversaService;

    @GetMapping
    public Map<String, Object> listar(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return conversaService.contatos(authorization);
    }
}
