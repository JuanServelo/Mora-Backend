package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.model.Vaga;
import portaria.service.VagaService;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/vagas")
@RequiredArgsConstructor
public class VagaController {

    private final VagaService vagaService;

    @PostMapping("/cadastrar")
    public ResponseEntity<Vaga> cadastrar(@Valid @RequestBody Vaga vaga) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vagaService.cadastrar(vaga));
    }

    @GetMapping
    public List<Vaga> listarApenasAtivas() {
        return vagaService.listarApenasAtivas();
    }

    @GetMapping("/todas")
    public List<Vaga> listarTodas() {
        return vagaService.listarTodas();
    }

    @GetMapping("/{id}")
    public Vaga buscarPorId(@PathVariable String id) {
        return vagaService.buscarPorId(id);
    }

    @GetMapping("/apartamento/{apartamentoId}")
    public List<Vaga> buscarPorApartamento(@PathVariable UUID apartamentoId) {
        return vagaService.buscarPorApartamento(apartamentoId);
    }

    @PutMapping("/{id}")
    public Vaga atualizar(@PathVariable String id, @Valid @RequestBody Vaga dados) {
        return vagaService.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable String id) {
        vagaService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable String id) {
        vagaService.ativar(id);
        return ResponseEntity.noContent().build();
    }
}
