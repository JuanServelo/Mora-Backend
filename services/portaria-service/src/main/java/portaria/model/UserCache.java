package portaria.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Cache local de usuários do Banco AUTH (docs/assincrona.md).
 *
 * Os usuários residem exclusivamente no Banco AUTH; o Banco MORA mantém esta
 * cópia para conseguir referenciá-los (ocupantes, multas, reservas) sem FK
 * cruzada entre bancos. É alimentada de forma assíncrona pelos eventos
 * `user.created` / `user.updated` / `user.deactivated`, com consistência eventual.
 */
@Data
@Entity
@Table(name = "user_cache")
public class UserCache {

    @Id
    @Column(name = "user_id")
    private Long userId;

    private String nome;

    private String email;

    private String perfil;

    @Column(name = "condominio_id")
    private String condominioId;

    @Column(name = "unidade_id")
    private String unidadeId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
