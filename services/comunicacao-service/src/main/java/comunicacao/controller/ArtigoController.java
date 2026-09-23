package comunicacao.controller;

import comunicacao.model.ArtigoConhecimento;
import comunicacao.model.enums.CategoriaArtigo;
import comunicacao.security.CondominioUtils;
import comunicacao.service.ArtigoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/artigos")
@RequiredArgsConstructor
public class ArtigoController {

    private final ArtigoService artigoService;

    @PostMapping
    public ResponseEntity<ArtigoConhecimento> criar(@Valid @RequestBody ArtigoConhecimento artigo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(artigoService.criar(artigo));
    }

    @GetMapping
    public List<ArtigoConhecimento> listar(
            @RequestParam(required = false) boolean publicadosOnly,
            @RequestParam(required = false) CategoriaArtigo categoria) {
        String condominioId = CondominioUtils.condominioIdEfetivo();
        if (publicadosOnly || categoria != null) {
            return artigoService.listarPublicados(condominioId, categoria);
        }
        return artigoService.listarTodos(condominioId);
    }

    /** Busca por titulo — o morador so alcanca o que ja foi publicado. */
    @GetMapping("/buscar")
    public List<ArtigoConhecimento> buscar(@RequestParam String titulo) {
        return artigoService.buscarPorTitulo(titulo, CondominioUtils.condominioIdEfetivo());
    }

    @GetMapping("/{id}")
    public ArtigoConhecimento buscarPorId(@PathVariable UUID id) {
        return artigoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public ArtigoConhecimento atualizar(@PathVariable UUID id, @Valid @RequestBody ArtigoConhecimento dados) {
        return artigoService.atualizar(id, dados);
    }

    @PatchMapping("/{id}/publicar")
    public ArtigoConhecimento publicar(@PathVariable UUID id) {
        return artigoService.publicar(id);
    }

    @PatchMapping("/{id}/despublicar")
    public ArtigoConhecimento despublicar(@PathVariable UUID id) {
        return artigoService.despublicar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID id) {
        artigoService.excluir(id);
    }
}
