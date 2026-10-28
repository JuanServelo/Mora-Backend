package ocorrencias.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ocorrencias.model.enums.StatusReclamacao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ocorrência registrada por um morador.
 *
 * Migrada do `auth_db.reclamacoes` (ver `migration/migrar-reclamacoes-auth-db.sql`),
 * com o mesmo `id` e o mesmo protocolo: o número que o morador anotou continua
 * valendo.
 */
@Getter
@Setter
@Entity
@Table(name = "reclamacoes", indexes = {
        @Index(name = "idx_reclamacoes_condominio", columnList = "condominio_id, status"),
        @Index(name = "idx_reclamacoes_usuario", columnList = "usuario_id")
})
public class Reclamacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * O id de quem abriu, como texto: o auth-api numera usuários com inteiro
     * hoje, e a decisão D28 prevê UUID. `VARCHAR` aceita os dois.
     */
    @Column(name = "usuario_id", nullable = false, length = 64)
    private String usuarioId;

    @Column(name = "condominio_id", nullable = false, length = 50)
    private String condominioId;

    @Column(nullable = false, unique = true, length = 40)
    private String protocolo;

    @Column(nullable = false, length = 40)
    private String categoria;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "anexo_url", length = 500)
    private String anexoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusReclamacao status = StatusReclamacao.PENDENTE;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    /**
     * Quando passou a `RESOLVIDO`. Antes o tempo de resolução era estimado
     * pelo `updatedAt`, que mudava a qualquer alteração.
     */
    @Column(name = "resolvido_em")
    private LocalDateTime resolvidoEm;

    /** O histórico de atendimento — antes um JSON solto, sem como consultar. */
    @OneToMany(mappedBy = "reclamacao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("criadoEm ASC, id ASC")
    private List<ReclamacaoInteracao> interacoes = new ArrayList<>();

    @PrePersist
    void aoCriar() {
        LocalDateTime agora = LocalDateTime.now();
        if (criadoEm == null) criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    void aoAlterar() {
        atualizadoEm = LocalDateTime.now();
    }

    public void adicionarInteracao(ReclamacaoInteracao interacao) {
        interacao.setReclamacao(this);
        interacoes.add(interacao);
    }
}
