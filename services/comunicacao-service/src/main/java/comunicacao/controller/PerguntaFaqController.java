package comunicacao.controller;

import comunicacao.dto.FaqDTOs.PerguntaRequest;
import comunicacao.dto.FaqDTOs.RespostaRequest;
import comunicacao.model.PerguntaFaq;
import comunicacao.model.enums.StatusPergunta;
import comunicacao.service.PerguntaFaqService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Perguntas que o morador não encontrou na FAQ.
 *
 * O morador pergunta e acompanha as próprias; a administração responde a fila
 * do condomínio e, quando a dúvida serve para todos, publica a resposta.
 */
@RestController
@RequestMapping("/faq/perguntas")
@RequiredArgsConstructor
@Tag(name = "Perguntas da FAQ", description = "Dúvidas que o morador não encontrou na FAQ. A administração responde e, quando serve para todos, publica como artigo.")
public class PerguntaFaqController {

    private final PerguntaFaqService perguntaService;

    @PostMapping
    @Operation(summary = "Fazer pergunta", description = "O morador envia uma dúvida (10 a 1000 caracteres) para a administração do condomínio.")
    @ApiResponse(responseCode = "201", description = "Pergunta registrada.")
    public ResponseEntity<PerguntaFaq> perguntar(@Valid @RequestBody PerguntaRequest dados) {
        return ResponseEntity.status(HttpStatus.CREATED).body(perguntaService.perguntar(dados));
    }

    @GetMapping("/minhas")
    @Operation(summary = "Minhas perguntas", description = "As perguntas que quem chama fez, com a resposta quando houver.")
    @ApiResponse(responseCode = "200", description = "Perguntas.")
    public List<PerguntaFaq> minhas() {
        return perguntaService.minhas();
    }

    /** Só a gestão. `status` opcional: PENDENTE ou RESPONDIDA. */
    @GetMapping
    @Operation(summary = "Perguntas do condomínio", description = "A fila da administração, filtrável por status (PENDENTE ou RESPONDIDA). Perfis: ADMIN_SINDICO e ADMIN_GERAL.")
    @ApiResponse(responseCode = "200", description = "Perguntas.")
    public List<PerguntaFaq> doCondominio(@Parameter(description = "Filtra pelo status.", example = "PENDENTE") @RequestParam(required = false) StatusPergunta status) {
        return perguntaService.doCondominio(status);
    }

    @PostMapping("/{id}/resposta")
    @Operation(summary = "Responder pergunta", description = """
            Grava a resposta. Com `publicar: true`, a resposta vira um artigo da FAQ, com o título e a categoria \
            informados. Perfis: ADMIN_SINDICO e ADMIN_GERAL.""")
    @ApiResponse(responseCode = "200", description = "Pergunta respondida.")
    public PerguntaFaq responder(@Parameter(description = "Id da pergunta.", example = "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d") @PathVariable UUID id, @Valid @RequestBody RespostaRequest dados) {
        return perguntaService.responder(id, dados);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir pergunta", description = "Apaga a pergunta.")
    @ApiResponse(responseCode = "204", description = "Excluída.")
    public void excluir(@Parameter(description = "Id da pergunta.", example = "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d") @PathVariable UUID id) {
        perguntaService.excluir(id);
    }
}
