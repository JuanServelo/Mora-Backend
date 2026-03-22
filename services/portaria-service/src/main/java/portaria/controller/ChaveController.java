package portaria.controller;

import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import portaria.model.Chave;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/chaves")
public class ChaveController {

    private List<Chave> chaves = new ArrayList<>();

    @PostMapping("/cadastrar")
    public Chave cadastrarChave(@RequestBody Chave novaChave) {
        chaves.add(novaChave);
        return novaChave;
    }

    @PostMapping("/deletar/{id}")
    public String deletarChave(@PathVariable String id) {
        Optional<Chave> chave = chaves.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst();

        if (chave.isPresent()) {
            chaves.remove(chave.get());
            return "Chave deletada com sucesso.";
        } else {
            return "Chave não encontrada.";
        }
    }

    @PostMapping("/retirar/{id}")
    public String retirarChave(@PathVariable String id, @RequestBody String responsavel) {
        Optional<Chave> chave = chaves.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst();

        if (chave.isPresent() && chave.get().isDisponivel()) {
            chave.get().setDisponivel(false);
            chave.get().setResponsavel(responsavel);
            chave.get().setRetirada(LocalDateTime.now());
            return "Chave retirada com sucesso por " + responsavel;
        } else {
            return "Chave não encontrada ou indisponível.";
        }
    }

    @PostMapping("/devolver/{id}")
    public String devolverChave(@PathVariable String id) {
        Optional<Chave> chave = chaves.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst();

        if (chave.isPresent() && !chave.get().isDisponivel()) {
            chave.get().setDisponivel(true);
            chave.get().setDevolucao(LocalDateTime.now());
            chave.get().setResponsavel(null);
            return "Chave devolvida com sucesso.";
        } else {
            return "Chave não encontrada ou já disponível.";
        }
    }

    @GetMapping("/procurar")
    public List<Chave> listarChaves() {
        return chaves;
    }

    @GetMapping("/procurar/{id}")
    public Chave buscarChavePorId(@PathVariable String id) {
        if (id == null || id.isEmpty()) {
            return null;
        } else {
            return chaves.stream()
                    .filter(c -> c.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        }
    }

}