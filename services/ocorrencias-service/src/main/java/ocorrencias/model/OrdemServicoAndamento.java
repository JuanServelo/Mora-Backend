package ocorrencias.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ocorrencias.model.enums.StatusOrdemServico;

import java.time.LocalDateTime;

/**
 * Registro de andamento da OS, com data e autor.
 *
 * Também registra o que o sistema faz sozinho — a marcação de atraso pelo job
 * entra aqui com `autorId` nulo, para o histórico contar a história inteira.
 */
@Getter
@Setter
@Entity
@Table(name = "ordem_servico_andamentos", indexes = {
        @Index(name = "idx_os_andamentos_ordem", columnList = "ordem_servico_id")
})
public class OrdemServicoAndamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    private OrdemServico ordemServico;

    /** Nulo quando foi o sistema. */
    @Column(name = "autor_id", length = 64)
    private String autorId;

    @Column(name = "autor_email", length = 255)
    private String autorEmail;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 20)
    private StatusOrdemServico statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", length = 20)
    private StatusOrdemServico statusNovo;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void aoCriar() {
        if (criadoEm == null) criadoEm = LocalDateTime.now();
    }
}
