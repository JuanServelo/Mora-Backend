package comunicacao.controller;

import comunicacao.dto.FaqDTOs.AvaliacaoRequest;
import comunicacao.dto.FaqDTOs.AvaliacaoResumo;
import comunicacao.security.UsuarioAtual;
import comunicacao.service.ArtigoAvaliacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** "Essa resposta ajudou?" nos artigos da base de conhecimento. */
@RestController
@RequestMapping("/artigos")
@RequiredArgsConstructor
public class ArtigoAvaliacaoController {

    private final ArtigoAvaliacaoService avaliacaoService;

    /** Totais por artigo do condomínio, com o voto de quem pediu. */
    @GetMapping("/avaliacoes")
    public List<AvaliacaoResumo> resumo() {
        return avaliacaoService.resumoDoCondominio(UsuarioAtual.id());
    }

    @PutMapping("/{id}/avaliacao")
    public AvaliacaoResumo avaliar(@PathVariable UUID id, @Valid @RequestBody AvaliacaoRequest dados) {
        return avaliacaoService.avaliar(id, UsuarioAtual.id(), dados.util());
    }

    @DeleteMapping("/{id}/avaliacao")
    public AvaliacaoResumo removerAvaliacao(@PathVariable UUID id) {
        return avaliacaoService.removerAvaliacao(id, UsuarioAtual.id());
    }
}
