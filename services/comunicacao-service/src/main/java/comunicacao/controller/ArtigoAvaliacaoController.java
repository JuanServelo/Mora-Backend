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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** "Essa resposta ajudou?" nos artigos da base de conhecimento. */
@RestController
@RequestMapping("/artigos")
@RequiredArgsConstructor
@Tag(name = "Base de conhecimento", description = "Artigos e FAQ do condomínio (RF-12): regras, manuais, tutoriais e orientações de convivência.")
public class ArtigoAvaliacaoController {

    private final ArtigoAvaliacaoService avaliacaoService;

    /** Totais por artigo do condomínio, com o voto de quem pediu. */
    @GetMapping("/avaliacoes")
    @Operation(summary = "Avaliações dos artigos", description = """
            Para cada artigo do condomínio, quantos acharam útil e quantos não, com o voto de quem chama \
            ("Essa resposta ajudou?").""")
    @ApiResponse(responseCode = "200", description = "Totais por artigo.")
    public List<AvaliacaoResumo> resumo() {
        return avaliacaoService.resumoDoCondominio(UsuarioAtual.id());
    }

    @PutMapping("/{id}/avaliacao")
    @Operation(summary = "Avaliar artigo", description = "Registra ou troca o voto de quem chama: útil ou não. Um voto por pessoa e artigo.")
    @ApiResponse(responseCode = "200", description = "Totais atualizados.")
    public AvaliacaoResumo avaliar(@Parameter(description = "Id do artigo.", example = "c4d2e8f1-5a7b-4c3d-9e1f-2b6a8d0c4e7f") @PathVariable UUID id, @Valid @RequestBody AvaliacaoRequest dados) {
        return avaliacaoService.avaliar(id, UsuarioAtual.id(), dados.util());
    }

    @DeleteMapping("/{id}/avaliacao")
    @Operation(summary = "Retirar avaliação", description = "Apaga o voto de quem chama no artigo.")
    @ApiResponse(responseCode = "200", description = "Totais atualizados.")
    public AvaliacaoResumo removerAvaliacao(@Parameter(description = "Id do artigo.", example = "c4d2e8f1-5a7b-4c3d-9e1f-2b6a8d0c4e7f") @PathVariable UUID id) {
        return avaliacaoService.removerAvaliacao(id, UsuarioAtual.id());
    }
}
