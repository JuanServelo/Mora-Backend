package comunicacao.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de que um usuário leu um aviso.
 *
 * É o que permite ao síndico comprovar ciência de uma regra. A unicidade
 * (aviso, usuário) faz a marcação ser idempotente: reabrir o aviso não gera
 * uma segunda leitura nem move a data da primeira.
 */
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

    /** Id do usuário no auth-api, que é um inteiro — guardado como texto. */
    @Column(name = "usuario_id", nullable = false, length = 64)
    private String usuarioId;

    @Column(name = "lido_em")
    private LocalDateTime lidoEm = LocalDateTime.now();
}
