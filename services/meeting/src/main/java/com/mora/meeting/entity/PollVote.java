package com.mora.meeting.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_poll_vote", uniqueConstraints = @UniqueConstraint(
        name = "uq_poll_vote_unidade", columnNames = {"poll_id", "unidade_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID do usuário que vem do microserviço do seu colega
    @Column(nullable = false)
    private Long usuarioId;

    // Nullable only for historical votes whose unit cannot be reconstructed safely.
    @Column(name = "unidade_id", length = 100)
    private String unidadeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_option_id", nullable = false)
    private PollOption pollOption;
}
