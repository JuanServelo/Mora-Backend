package comunicacao.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * "Essa resposta ajudou?" — um voto por usuário em cada artigo.
 *
 * O voto pode ser trocado (a linha é atualizada) ou retirado (a linha some), e
 * a restrição única garante que ninguém conta duas vezes.
 */
@Data
@Entity
@Table(name = "artigo_avaliacoes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"artigo_id", "usuario_id"}))
public class ArtigoAvaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "artigo_id", nullable = false)
    private UUID artigoId;

    @Column(name = "usuario_id", nullable = false, length = 64)
    private String usuarioId;

    @Column(nullable = false)
    private boolean util;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
