package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.dto.ArtigoConhecimentoRequestDTO;
import portaria.dto.ArtigoConhecimentoResponseDTO;
import portaria.model.enums.CategoriaArtigo;
import portaria.service.ArtigoConhecimentoService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/conhecimento")
@RequiredArgsConstructor
public class ArtigoConhecimentoController {

    private final ArtigoConhecimentoService service;

    @PostMapping
    public ResponseEntity<ArtigoConhecimentoResponseDTO> criar(
            @Valid @RequestBody ArtigoConhecimentoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ArtigoConhecimentoResponseDTO.fromEntity(service.criar(request)));
    }

    @PutMapping("/{id}")
    public ArtigoConhecimentoResponseDTO atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ArtigoConhecimentoRequestDTO request) {
        return ArtigoConhecimentoResponseDTO.fromEntity(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<ArtigoConhecimentoResponseDTO> listarTodos() {
        return service.listarTodos().stream()
                .map(ArtigoConhecimentoResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/publicados")
    public List<ArtigoConhecimentoResponseDTO> listarPublicados() {
        return service.listarPublicados().stream()
                .map(ArtigoConhecimentoResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/categoria/{categoria}")
    public List<ArtigoConhecimentoResponseDTO> listarPorCategoria(
            @PathVariable CategoriaArtigo categoria) {
        return service.listarPorCategoria(categoria).stream()
                .map(ArtigoConhecimentoResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/categoria/{categoria}/publicados")
    public List<ArtigoConhecimentoResponseDTO> listarPublicadosPorCategoria(
            @PathVariable CategoriaArtigo categoria) {
        return service.listarPublicadosPorCategoria(categoria).stream()
                .map(ArtigoConhecimentoResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/buscar")
    public List<ArtigoConhecimentoResponseDTO> buscarPorTitulo(@RequestParam String titulo) {
        return service.buscarPorTitulo(titulo).stream()
                .map(ArtigoConhecimentoResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public ArtigoConhecimentoResponseDTO buscarPorId(@PathVariable UUID id) {
        return ArtigoConhecimentoResponseDTO.fromEntity(service.buscarPorId(id));
    }
}
