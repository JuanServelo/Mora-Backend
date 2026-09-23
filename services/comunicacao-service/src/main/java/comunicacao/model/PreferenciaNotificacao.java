package comunicacao.model;

import jakarta.persistence.*;
import lombok.Data;
import comunicacao.model.enums.TipoNotificacao;

/**
 * Categoria que o usuário optou por não receber.
 *
 * Guardamos só as recusas: a ausência de linha significa "recebe", então um
 * usuário novo já nasce recebendo tudo sem precisar de carga inicial.
 */
@Data
@Entity
@Table(name = "preferencias_notificacao",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "tipo"}))
public class PreferenciaNotificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "usuario_id", nullable = false, length = 64)
    private String usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNotificacao tipo;
}
