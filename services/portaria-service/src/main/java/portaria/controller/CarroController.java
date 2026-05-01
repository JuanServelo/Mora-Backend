package portaria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portaria.model.Carro;
import portaria.dto.CriarCarroDTO;
import portaria.service.CarroService;

import java.util.List;

@RestController
@RequestMapping("/carros")
@RequiredArgsConstructor
public class CarroController {

    private final CarroService carroService;

    @PostMapping("/cadastrar")
    public ResponseEntity<Carro> cadastrar(@Valid @RequestBody CriarCarroDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(carroService.cadastrar(dto));
    }

    @PostMapping("/entrada")
    public ResponseEntity<Carro> registrarEntrada(@Valid @RequestBody Carro carro) {
        return ResponseEntity.status(HttpStatus.CREATED).body(carroService.registrarEntrada(carro));
    }

    @PostMapping("/{id}/saida")
    public Carro registrarSaida(@PathVariable String id) {
        return carroService.registrarSaida(id);
    }

    @GetMapping
    public List<Carro> listarTodos() {
        return carroService.listarTodos();
    }

    @GetMapping("/dentro")
    public List<Carro> listarDentro() {
        return carroService.listarDentro();
    }

    @GetMapping("/{id}")
    public Carro buscarPorId(@PathVariable String id) {
        return carroService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public Carro atualizar(@PathVariable String id, @Valid @RequestBody CriarCarroDTO dados) {
        return carroService.atualizar(id, dados);
    }
}
