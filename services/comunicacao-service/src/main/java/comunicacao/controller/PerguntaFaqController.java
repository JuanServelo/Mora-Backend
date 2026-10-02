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

/**
 * Perguntas que o morador não encontrou na FAQ.
 *
 * O morador pergunta e acompanha as próprias; a administração responde a fila
 * do condomínio e, quando a dúvida serve para todos, publica a resposta.
 */
@RestController
@RequestMapping("/faq/perguntas")
@RequiredArgsConstructor
public class PerguntaFaqController {

    private final PerguntaFaqService perguntaService;

    @PostMapping
    public ResponseEntity<PerguntaFaq> perguntar(@Valid @RequestBody PerguntaRequest dados) {
        return ResponseEntity.status(HttpStatus.CREATED).body(perguntaService.perguntar(dados));
    }

    @GetMapping("/minhas")
    public List<PerguntaFaq> minhas() {
        return perguntaService.minhas();
    }

    /** Só a gestão. `status` opcional: PENDENTE ou RESPONDIDA. */
    @GetMapping
    public List<PerguntaFaq> doCondominio(@RequestParam(required = false) StatusPergunta status) {
        return perguntaService.doCondominio(status);
    }

    @PostMapping("/{id}/resposta")
    public PerguntaFaq responder(@PathVariable UUID id, @Valid @RequestBody RespostaRequest dados) {
        return perguntaService.responder(id, dados);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID id) {
        perguntaService.excluir(id);
    }
}
