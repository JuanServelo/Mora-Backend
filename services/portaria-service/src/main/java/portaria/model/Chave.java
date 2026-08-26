package portaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import portaria.model.enums.TipoResponsavel;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chaves")
public class Chave {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Nome da chave é obrigatório")
    @Column(name = "nome_chave")
    private String nomeChave;

    @Column(name = "responsavel_id")
    private String responsavelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_responsavel")
    private TipoResponsavel tipoResponsavel;

    @Column(name = "nome_responsavel")
    private String nomeResponsavel;

    /** Cliente dono deste registro. Todo dado de domínio pertence a um condomínio. */
    @Column(name = "`condominioId`")
    private String condominioId;

    private LocalDateTime retirada;

    private LocalDateTime devolucao;

    private boolean disponivel = true;
}
