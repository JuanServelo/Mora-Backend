package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.dto.chave.RetirarChaveRequest;
import portaria.model.Chave;
import portaria.service.ChaveService;

import java.util.List;

@RestController
@RequestMapping("/chaves")
@RequiredArgsConstructor
public class ChaveController {

    private final ChaveService chaveService;

    @PostMapping("/cadastrar")
    public ResponseEntity<Chave> cadastrar(@Valid @RequestBody Chave chave) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chaveService.cadastrar(chave));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable String id) {
        chaveService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/retirar")
    public Chave retirar(@PathVariable String id, @Valid @RequestBody RetirarChaveRequest request) {
        return chaveService.retirar(id, request);
    }

    @PostMapping("/{id}/devolver")
    public Chave devolver(@PathVariable String id) {
        return chaveService.devolver(id);
    }

    @GetMapping
    public List<Chave> listarTodas() {
        return chaveService.listarTodas();
    }

    @GetMapping("/disponiveis")
    public List<Chave> listarDisponiveis() {
        return chaveService.listarDisponiveis();
    }

    @GetMapping("/{id}")
    public Chave buscarPorId(@PathVariable String id) {
        return chaveService.buscarPorId(id);
    }
}
