package ocorrencias.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Prestador de serviço acionado nas ordens de serviço.
 *
 * Não se exclui: uma OS concluída continua apontando para quem a executou.
 * Quem não presta mais serviço fica `ativo = false` e some da escolha de novas OS.
 */
@Getter
@Setter
@Entity
@Table(name = "fornecedores", indexes = {
        @Index(name = "idx_fornecedores_condominio", columnList = "condominio_id")
})
public class Fornecedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "condominio_id", nullable = false, length = 50)
    private String condominioId;

    @Column(nullable = false, length = 150)
    private String nome;

    /** O que ele faz: "Elétrica", "Elevadores", "Jardinagem"... */
    @Column(length = 100)
    private String servico;

    @Column(length = 20)
    private String telefone;

    @Column(length = 150)
    private String email;

    /** CNPJ ou CPF, opcional. */
    @Column(length = 20)
    private String documento;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void aoCriar() {
        if (criadoEm == null) criadoEm = LocalDateTime.now();
    }
}
