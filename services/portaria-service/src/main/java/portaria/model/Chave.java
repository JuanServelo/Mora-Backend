package portaria.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class Chave {
    private String id;
    private String nomeChave;
    private String responsavel;
    private LocalDateTime retirada;
    private LocalDateTime devolucao;
    private boolean disponivel;

    public Chave(String nomeChave) {
        this.id = UUID.randomUUID().toString();
        this.nomeChave = nomeChave;
        this.disponivel = true;
    }
}