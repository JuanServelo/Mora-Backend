package portaria.controller;

import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import portaria.model.Entrega;

@RestController
@RequestMapping("/entregas")
public class EntregaController {

    private List<Entrega> entregas = new ArrayList<>();

    @PostMapping("/cadastrar")
    public Entrega registrarEntrega(@RequestBody Entrega novaEntrega) {
        entregas.add(novaEntrega);
        return novaEntrega;
    }

    @GetMapping("/procurar")
    public List<Entrega> listarEntregas() {
        return entregas;
    }

    @GetMapping("/procurar/{id}")
    public Entrega buscarEntregaPorId(@PathVariable String id) {
        return entregas.stream()
                       .filter(e -> e.getId().equals(id))
                       .findFirst()
                       .orElse(null);
    }

    @PostMapping("/retirar/{id}")
    public String retirarEntrega(@PathVariable String id, @RequestBody String recebedor) {
        Optional<Entrega> entrega = entregas.stream()
                                          .filter(e -> e.getId().equals(id))
                                          .findFirst();

        if (entrega.isPresent() && !entrega.get().isRetirada()) {
            entrega.get().setRetirada(true);
            entrega.get().setDataRetirada(java.time.LocalDateTime.now());
            entrega.get().setRecebedor(recebedor);
            return "Entrega retirada com sucesso por " + recebedor; //adicionar a funcao de disparada de email/notificacao
        } else {
            return "Entrega não encontrada ou já retirada.";
        }
    }
}