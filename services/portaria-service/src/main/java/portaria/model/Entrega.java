package portaria.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class Entrega {
    private String id;
    private String nomeEntregador;
    private String destinatario;
    private String recebedor;
    private String descricao;
    private LocalDateTime dataRecebimento;
    private LocalDateTime dataRetirada;
    private boolean retirada;

    public Entrega(String nomeEntregador, String destinatario, String descricao) {
        this.id = UUID.randomUUID().toString();
        this.nomeEntregador = nomeEntregador;
        this.destinatario = destinatario;
        this.descricao = descricao;
        this.dataRecebimento = LocalDateTime.now();
        this.dataRetirada = null;
        this.recebedor = null;
        this.retirada = false;
    }
}