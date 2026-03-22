package portaria.controller;

import org.springframework.web.bind.annotation.*;

import portaria.model.Visitante;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/visitantes")
public class VisitanteController {

    private List<Visitante> visitantes = new ArrayList<>();

    @PostMapping("/cadastrar")
    public Visitante cadastrarVisitante(@RequestBody Visitante visitante) {
        visitantes.add(visitante);
        return visitante;
    }

    @GetMapping("/procurar/{id}")
    public Visitante buscarVisitantePorId(@PathVariable String id) {
        return visitantes.stream()
                         .filter(v -> v.getId().equals(id))
                         .findFirst()
                         .orElse(null);
    }

    @GetMapping("/procurar")
    public List<Visitante> listarVisitantes() {
        return visitantes;
    }

}