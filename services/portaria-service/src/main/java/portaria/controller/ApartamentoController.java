package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.model.Apartamento;
import portaria.service.ApartamentoService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/apartamentos")
@RequiredArgsConstructor
public class ApartamentoController {

    private final ApartamentoService apartamentoService;

    @PostMapping("/cadastrar")
    public ResponseEntity<Apartamento> cadastrar(
            @Valid @RequestBody Apartamento apartamento,
            @RequestParam UUID blocoId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apartamentoService.cadastrar(apartamento, blocoId));
    }

    @GetMapping
    public List<Apartamento> listarAtivos() {
        return apartamentoService.listarAtivos();
    }

    @GetMapping("/todos")
    public List<Apartamento> listarTodos() {
        return apartamentoService.listarTodos();
    }

    @GetMapping("/{id}")
    public Apartamento buscarPorId(@PathVariable UUID id) {
        return apartamentoService.buscarPorId(id);
    }

    @GetMapping("/bloco/{blocoId}")
    public List<Apartamento> listarPorBloco(@PathVariable UUID blocoId) {
        return apartamentoService.listarPorBloco(blocoId);
    }

    @GetMapping("/bloco/{blocoId}/ativos")
    public List<Apartamento> listarPorBlocoAtivos(@PathVariable UUID blocoId) {
        return apartamentoService.listarPorBlocoAtivos(blocoId);
    }

    @PutMapping("/{id}")
    public Apartamento atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody Apartamento dados,
            @RequestParam UUID blocoId
    ) {
        return apartamentoService.atualizar(id, dados, blocoId);
    }

    @PutMapping("/{id}/desativar")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        apartamentoService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable UUID id) {
        apartamentoService.ativar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        apartamentoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}