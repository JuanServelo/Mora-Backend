package portaria.model;

import lombok.Data;
import java.util.UUID;

@Data
public class Carro {
    private String id;
    private String placa;
    private String modelo;
    private String proprietario;

    public Carro() {
        this.id = UUID.randomUUID().toString();
    }
}