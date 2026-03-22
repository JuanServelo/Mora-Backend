package portaria.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class Turno {
    private String id;
    private String funcionario;
    private String cargo;
    private List<LocalDateTime> entradas;
    private List<LocalDateTime> saidas;

    public Turno(String funcionario, String cargo) {
        this.id = UUID.randomUUID().toString();
        this.funcionario = funcionario;
        this.cargo = cargo;
        this.entradas = new ArrayList<>();
        this.saidas = new ArrayList<>();
        this.entradas.add(LocalDateTime.now());
    }
}