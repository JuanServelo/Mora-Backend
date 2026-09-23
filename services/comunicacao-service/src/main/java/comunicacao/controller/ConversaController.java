package comunicacao.controller;

import comunicacao.service.ConversaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
public class ConversaController {

    private final ConversaService conversaService;

    @GetMapping
    public Map<String, Object> listar(
            @RequestParam(name = "encerradas", defaultValue = "false") boolean encerradas,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return conversaService.listar(encerradas, authorization);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> abrir(
            @RequestBody ConversaService.NovaConversa corpo,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conversaService.abrir(corpo, authorization));
    }

    @GetMapping("/{id}")
    public Map<String, Object> detalhar(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return conversaService.detalhar(id, authorization);
    }

    @PostMapping("/{id}/mensagens")
    public ResponseEntity<Map<String, Object>> responder(
            @PathVariable Long id,
            @RequestBody Map<String, String> corpo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conversaService.responder(id, corpo.get("corpo")));
    }

    @PatchMapping("/{id}/encerrar")
    public Map<String, Object> encerrar(@PathVariable Long id) {
        return conversaService.encerrar(id);
    }
}
