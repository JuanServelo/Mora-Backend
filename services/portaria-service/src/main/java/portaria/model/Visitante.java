package portaria.model;

import lombok.Data;
import java.util.UUID;

@Data
public class Visitante {
    private String id;
    private String nome;
    private String documento;
    private String motivoVisita;

    public Visitante() {
        this.id = UUID.randomUUID().toString();
    }
}