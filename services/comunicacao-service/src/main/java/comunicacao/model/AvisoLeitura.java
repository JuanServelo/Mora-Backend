package comunicacao.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "aviso_leituras",
        uniqueConstraints = @UniqueConstraint(columnNames = {"aviso_id", "usuario_id"}))
public class AvisoLeitura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "aviso_id", nullable = false)
    private UUID avisoId;


    /**
     * O id de quem, como texto.
     *
     * Não é UUID: o `auth-api` numera usuários com `integer` autoincremental, e
     * era isso que o token trazia — `UUID.fromString("32")` derrubava com 500
     * toda rota que precisa saber quem está pedindo. `VARCHAR` aceita o formato
     * de hoje e sobrevive a uma eventual migração para UUID sem nova alteração
     * de esquema; o preço é não haver tipo forte no banco.
     */
    @Column(name = "usuario_id", nullable = false)
    private String usuarioId;

    @Column(name = "lido_em")
    private LocalDateTime lidoEm = LocalDateTime.now();
}
