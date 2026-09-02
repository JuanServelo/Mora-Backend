package com.mora.plan.entity;

import com.mora.plan.enums.StatusAssinatura;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Liga um condomínio ao plano que ele contratou.
 *
 * Sem esta entidade o catálogo de planos era decorativo: não havia como saber
 * qual plano cada condomínio tem, e por isso os limites `maxCondominiums`,
 * `maxUsersPerCondominium` e `activeModules` não eram aplicáveis.
 */
@Entity
@Table(
    name = "tb_assinaturas",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_assinatura_condominio_ativa",
        columnNames = {"condominio_id", "status"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assinatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Slug do condomínio, o mesmo usado como condominioId nos demais serviços. */
    @Column(name = "condominio_id", nullable = false, length = 50)
    private String condominioId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(name = "vigencia_inicio", nullable = false)
    private LocalDate vigenciaInicio;

    /** Nulo enquanto a assinatura não tem data de término definida. */
    @Column(name = "vigencia_fim")
    private LocalDate vigenciaFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusAssinatura status = StatusAssinatura.ATIVA;

    @Column(name = "observacao", length = 500)
    private String observacao;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.updatedAt == null) this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = StatusAssinatura.ATIVA;
        if (this.vigenciaInicio == null) this.vigenciaInicio = LocalDate.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** Vigente hoje: status ativo e dentro da janela de vigência. */
    public boolean estaVigente() {
        if (status != StatusAssinatura.ATIVA) return false;
        LocalDate hoje = LocalDate.now();
        if (vigenciaInicio != null && hoje.isBefore(vigenciaInicio)) return false;
        return vigenciaFim == null || !hoje.isAfter(vigenciaFim);
    }
}
