package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.model.Entrega;
import portaria.service.EntregaService;

import java.util.List;

@RestController
@RequestMapping("/entregas")
@RequiredArgsConstructor
public class EntregaController {

    private final EntregaService entregaService;

    @PostMapping("/registrar")
    public ResponseEntity<Entrega> registrar(@Valid @RequestBody Entrega entrega) {
        return ResponseEntity.status(HttpStatus.CREATED).body(entregaService.registrar(entrega));
    }

    @PostMapping("/{id}/retirar")
    public Entrega retirar(@PathVariable String id, @RequestBody String recebedor) {
        return entregaService.retirar(id, recebedor);
    }

    @GetMapping
    public List<Entrega> listarTodas() {
        return entregaService.listarTodas();
    }

    @GetMapping("/pendentes")
    public List<Entrega> listarPendentes() {
        return entregaService.listarPendentes();
    }

    @GetMapping("/{id}")
    public Entrega buscarPorId(@PathVariable String id) {
        return entregaService.buscarPorId(id);
    }
}
