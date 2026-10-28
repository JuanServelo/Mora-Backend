package ocorrencias.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ocorrencias.model.enums.StatusReclamacao;

import java.time.LocalDateTime;

/**
 * Uma entrada no histórico de atendimento: a resposta do síndico e, quando
 * houve, a mudança de status — com autor, data e justificativa.
 *
 * `statusAnterior` igual a `statusNovo` (ou os dois nulos) é só resposta, sem
 * mudança.
 */
@Getter
@Setter
@Entity
@Table(name = "reclamacao_interacoes", indexes = {
        @Index(name = "idx_reclamacao_interacoes_reclamacao", columnList = "reclamacao_id")
})
public class ReclamacaoInteracao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reclamacao_id", nullable = false)
    private Reclamacao reclamacao;

    /** Nulo nas interações migradas do JSON antigo, que não guardava autor. */
    @Column(name = "autor_id", length = 64)
    private String autorId;

    @Column(name = "autor_email", length = 255)
    private String autorEmail;

    @Column(name = "autor_perfil", length = 30)
    private String autorPerfil;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 20)
    private StatusReclamacao statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", length = 20)
    private StatusReclamacao statusNovo;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void aoCriar() {
        if (criadoEm == null) criadoEm = LocalDateTime.now();
    }
}
