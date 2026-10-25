package comunicacao.controller;

import comunicacao.service.ConversaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "Conversas", description = "Conversas com assunto: o morador falando com a administração (ADMINISTRACAO) ou a gestão com uma pessoa (DIRETA).")
public class ContatoController {

    private final ConversaService conversaService;

    @GetMapping
    @Operation(summary = "Contatos para conversa direta", description = """
            Usuários do condomínio com quem a gestão pode abrir uma conversa direta (consulta o auth-api em nome de quem \
            chama). Perfis: ADMIN_SINDICO e ADMIN_GERAL.""")
    @ApiResponse(responseCode = "200", description = "Contatos.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            {
              "sucesso": true,
              "contatos": [
                { "id": "4", "nome": "Ana Souza", "email": "morador1.verde@mora.test", "perfil": "MORADOR",
                  "fotoUrl": "/uploads/avatars/avatar-4-1789421836000.jpg", "unidadeId": "e064d8a3-2835-4bdf-8e9f-06fa3a108e6d", "alcancavel": true }
              ]
            }""")))
    public Map<String, Object> listar(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return conversaService.contatos(authorization);
    }
}
