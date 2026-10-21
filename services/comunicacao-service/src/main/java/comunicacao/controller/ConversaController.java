package comunicacao.controller;

import comunicacao.service.ConversaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * As conversas, do ponto de vista de quem chama.
 *
 * O cabeçalho `Authorization` é repassado ao serviço porque a resolução de
 * nome e foto da contraparte é feita **em nome de quem pediu**, contra o
 * auth-api. Sem repassar, o serviço teria que guardar credencial própria para
 * ler dado de usuário — e passaria a enxergar mais do que quem chamou enxerga.
 */
@RestController
@RequestMapping("/conversas")
@RequiredArgsConstructor
@Tag(name = "Conversas", description = "Conversas com assunto: o morador falando com a administração (ADMINISTRACAO) ou a gestão com uma pessoa (DIRETA).")
public class ConversaController {

    private final ConversaService conversaService;

    @GetMapping
    @Operation(summary = "Minhas conversas", description = """
            As conversas de quem chama, com a contraparte, um resumo da última mensagem e quantas não lidas. A gestão vê \
            todas as conversas ADMINISTRACAO do condomínio. `encerradas=true` inclui as encerradas. Qualquer perfil com \
            acesso ao sistema.""")
    @ApiResponse(responseCode = "200", description = "Conversas.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            {
              "sucesso": true,
              "conversas": [
                { "id": 12, "assunto": "Vazamento no teto do banheiro", "tipo": "ADMINISTRACAO", "criadaPor": "4",
                  "criadaEm": "2026-10-20T15:10:00", "ultimaMensagemEm": "2026-10-20T15:10:00", "encerradaEm": null,
                  "contraparte": { "id": "4", "nome": "Ana Souza", "email": "morador1.verde@mora.test", "perfil": "MORADOR", "fotoUrl": null, "unidadeId": "e064d8a3-2835-4bdf-8e9f-06fa3a108e6d", "alcancavel": true },
                  "resumo": "Boa tarde. Apareceu uma mancha de umidade no teto do banheiro do 101, acho que vem do 201.",
                  "naoLidas": 1 }
              ]
            }""")))
    public Map<String, Object> listar(
            @Parameter(description = "Incluir as encerradas.", example = "false") @RequestParam(name = "encerradas", defaultValue = "false") boolean encerradas,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return conversaService.listar(encerradas, authorization);
    }

    @PostMapping
    @Operation(summary = "Abrir conversa", description = """
            Abre a conversa já com a primeira mensagem. O morador abre ADMINISTRACAO (fala com a administração, não com \
            uma pessoa); só a gestão abre DIRETA, informando `destinatarioId`. Qualquer perfil com acesso ao sistema.""")
    @ApiResponse(responseCode = "201", description = "Conversa aberta.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            { "sucesso": true, "conversa": { "id": 12, "condominioId": "cond-parque-verde", "tipo": "ADMINISTRACAO", "assunto": "Vazamento no teto do banheiro", "criadaPor": "4", "ultimaMensagemEm": "2026-10-20T15:10:00", "encerradaEm": null, "criadaEm": "2026-10-20T15:10:00" }, "mensagem": { "id": 87, "conversaId": 12, "autorId": "4", "autorPerfil": "MORADOR", "corpo": "Boa tarde. Apareceu uma mancha de umidade no teto do banheiro do 101, acho que vem do 201.", "removidaEm": null, "criadaEm": "2026-10-20T15:10:00" } }""")))
    public ResponseEntity<Map<String, Object>> abrir(
            @RequestBody ConversaService.NovaConversa corpo,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conversaService.abrir(corpo, authorization));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe da conversa", description = """
            Cabeçalho, participantes e mensagens. Mensagem removida aparece com `corpo` nulo e `removidaEm`. Abrir a \
            conversa marca como lida para quem chama. Só participantes (e a gestão, nas ADMINISTRACAO).""")
    @ApiResponse(responseCode = "200", description = "Conversa.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            {
              "sucesso": true,
              "conversa": { "id": 12, "assunto": "Vazamento no teto do banheiro", "tipo": "ADMINISTRACAO", "criadaPor": "4",
                            "criadaEm": "2026-10-20T15:10:00", "ultimaMensagemEm": "2026-10-20T16:02:00", "encerradaEm": null,
                            "contraparte": { "id": "4", "nome": "Ana Souza", "email": "morador1.verde@mora.test", "perfil": "MORADOR", "fotoUrl": null, "unidadeId": "e064d8a3-2835-4bdf-8e9f-06fa3a108e6d", "alcancavel": true } },
              "participantes": [ { "usuarioId": "4", "ultimaLeituraEm": "2026-10-20T16:05:00", "pessoa": { "id": "4", "nome": "Ana Souza", "email": "morador1.verde@mora.test", "perfil": "MORADOR", "fotoUrl": null, "unidadeId": "e064d8a3-2835-4bdf-8e9f-06fa3a108e6d", "alcancavel": true } } ],
              "mensagens": [
                { "id": 87, "autorId": "4", "autorPerfil": "MORADOR", "corpo": "Boa tarde. Apareceu uma mancha de umidade no teto do banheiro do 101.",
                  "removidaEm": null, "criadaEm": "2026-10-20T15:10:00", "autor": { "id": "4", "nome": "Ana Souza", "email": "morador1.verde@mora.test", "perfil": "MORADOR", "fotoUrl": null, "unidadeId": "e064d8a3-2835-4bdf-8e9f-06fa3a108e6d", "alcancavel": true } },
                { "id": 88, "autorId": "2", "autorPerfil": "ADMIN_SINDICO", "corpo": "Obrigado, Ana. Vou pedir ao zelador para olhar o 201 amanhã cedo.",
                  "removidaEm": null, "criadaEm": "2026-10-20T16:02:00", "autor": null }
              ]
            }""")))
    public Map<String, Object> detalhar(
            @Parameter(description = "Id da conversa.", example = "12") @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return conversaService.detalhar(id, authorization);
    }

    @PostMapping("/{id}/mensagens")
    @Operation(summary = "Responder na conversa", description = "Acrescenta uma mensagem. Conversa encerrada não aceita resposta. Só participantes.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Texto da mensagem.", required = true,
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            { "corpo": "Obrigado, Ana. Vou pedir ao zelador para olhar o 201 amanhã cedo." }""")))
    @ApiResponse(responseCode = "201", description = "Mensagem enviada.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            { "sucesso": true, "mensagem": { "id": 88, "conversaId": 12, "autorId": "2", "autorPerfil": "ADMIN_SINDICO",
              "corpo": "Obrigado, Ana. Vou pedir ao zelador para olhar o 201 amanhã cedo.", "removidaEm": null, "criadaEm": "2026-10-20T16:02:00" } }""")))
    public ResponseEntity<Map<String, Object>> responder(
            @Parameter(description = "Id da conversa.", example = "12") @PathVariable Long id,
            @RequestBody Map<String, String> corpo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conversaService.responder(id, corpo.get("corpo")));
    }

    @PatchMapping("/{id}/encerrar")
    @Operation(summary = "Encerrar conversa", description = "Marca a conversa como encerrada: some da lista padrão e não aceita resposta. Só participantes.")
    @ApiResponse(responseCode = "200", description = "Conversa encerrada.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            { "sucesso": true, "conversa": { "id": 12, "condominioId": "cond-parque-verde", "tipo": "ADMINISTRACAO", "assunto": "Vazamento no teto do banheiro",
              "criadaPor": "4", "ultimaMensagemEm": "2026-10-20T16:02:00", "encerradaEm": "2026-10-21T09:00:00", "criadaEm": "2026-10-20T15:10:00" } }""")))
    public Map<String, Object> encerrar(@Parameter(description = "Id da conversa.", example = "12") @PathVariable Long id) {
        return conversaService.encerrar(id);
    }
}
