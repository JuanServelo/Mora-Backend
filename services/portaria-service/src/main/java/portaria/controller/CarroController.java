package portaria.controller;

import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import portaria.model.Carro;

@RestController
@RequestMapping("/carros")
public class CarroController {

    private List<Carro> carros = new ArrayList<>();

    @PostMapping("/cadastrar")
    public Carro registrarEntrada(@RequestBody Carro carro) {
        carros.add(carro);
        return carro;
    }

    @GetMapping("/procurar/{id}")
    public Carro buscarCarroPorId(@PathVariable String id) {
        return carros.stream()
                     .filter(c -> c.getId().equals(id))
                     .findFirst()
                     .orElse(null);
    }

    @GetMapping("/procurar")
    public List<Carro> listarCarros(@PathVariable String id) {
        return carros;
    }
    
}