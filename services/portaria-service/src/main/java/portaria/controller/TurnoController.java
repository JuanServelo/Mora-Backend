package portaria.controller;

import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import portaria.model.Turno;


@RestController
@RequestMapping("/turnos")
public class TurnoController {

    private List<Turno> turnos = new ArrayList<>();

    @PostMapping("/iniciar")
    public Turno iniciarTurno(@RequestBody Turno novoTurno) {
        if (novoTurno.getFuncionario() == null || novoTurno.getCargo() == null) {
            throw new IllegalArgumentException("Os campos 'funcionario' e 'cargo' são obrigatórios.");
        }
        Turno turno = new Turno(novoTurno.getFuncionario(), novoTurno.getCargo());
        turnos.add(turno);
        return turno;
    }

    @PostMapping("/retomar/{id}")
    public String retomarTurno(@PathVariable String id) {
        Optional<Turno> turno = turnos.stream()
                                      .filter(t -> t.getId().equals(id))
                                      .findFirst();

        if (turno.isPresent()) {
            if (turno.get().getSaidas().size() > turno.get().getEntradas().size()) {
                turno.get().getEntradas().add(LocalDateTime.now());
                turno.get().getSaidas().add(null);
                return "Turno retomado com sucesso.";
            } else {
                return "O turno já está em andamento.";
            }
        } else {
            return "Turno não encontrado.";
        }
    }
    
    @PostMapping("/finalizar/{id}")
    public String finalizarTurno(@PathVariable String id) {
        Optional<Turno> turno = turnos.stream()
                                      .filter(t -> t.getId().equals(id))
                                      .findFirst();
    
        if (turno.isPresent()) {
            if (turno.get().getSaidas().size() > turno.get().getEntradas().size()) {
                return "Turno já finalizado.";
            }
            turno.get().getSaidas().add(LocalDateTime.now());
            return "Turno finalizado com sucesso.";
        } else {
            return "Turno não encontrado.";
        }
    }

    @GetMapping("/listar")
    public List<Turno> listarTurnos() {
        return turnos;
    }

    @GetMapping("/listar/{id}")
    public Turno buscarTurnoPorId(@PathVariable String id) {
        return turnos.stream()
                     .filter(t -> t.getId().equals(id))
                     .findFirst()
                     .orElse(null);
    }
}