package ocorrencias.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ocorrencias.model.enums.PrioridadeOrdemServico;
import ocorrencias.model.enums.StatusOrdemServico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ordem de serviço.
 *
 * Nasce de uma ocorrência (`reclamacao` preenchida) ou avulsa, como manutenção
 * preventiva (`reclamacao` nula) — decisão D39.
 */
@Getter
@Setter
@Entity
@Table(name = "ordens_servico", indexes = {
        @Index(name = "idx_ordens_servico_condominio", columnList = "condominio_id, status"),
        @Index(name = "idx_ordens_servico_reclamacao", columnList = "reclamacao_id"),
        @Index(name = "idx_ordens_servico_prazo", columnList = "status, prazo")
})
public class OrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "condominio_id", nullable = false, length = 50)
    private String condominioId;

    @Column(nullable = false, unique = true, length = 40)
    private String numero;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrioridadeOrdemServico prioridade;

    @Column(nullable = false)
    private LocalDate prazo;

    /** Quem executa: o nome do prestador ou da pessoa da equipe. */
    @Column(nullable = false, length = 150)
    private String responsavel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_id")
    private Fornecedor fornecedor;

    /** A ocorrência de origem. Nula na manutenção preventiva. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reclamacao_id")
    private Reclamacao reclamacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOrdemServico status = StatusOrdemServico.ABERTA;

    /** O síndico que abriu — é a ele que o aviso de atraso chega. */
    @Column(name = "aberta_por_id", nullable = false, length = 64)
    private String abertaPorId;

    @Column(name = "aberta_por_email", length = 255)
    private String abertaPorEmail;

    @Column(precision = 12, scale = 2)
    private BigDecimal custo;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("criadoEm ASC, id ASC")
    private List<OrdemServicoAndamento> andamentos = new ArrayList<>();

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

    public boolean isPreventiva() {
        return reclamacao == null;
    }

    /** Concluída até o fim do dia do prazo. */
    public boolean isConcluidaNoPrazo() {
        return status == StatusOrdemServico.CONCLUIDA
                && concluidaEm != null
                && !concluidaEm.toLocalDate().isAfter(prazo);
    }

    public void adicionarAndamento(OrdemServicoAndamento andamento) {
        andamento.setOrdemServico(this);
        andamentos.add(andamento);
    }
}
