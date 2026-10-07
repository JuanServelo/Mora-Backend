package com.mora.meeting.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_poll_option")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @OneToMany(mappedBy = "pollOption", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<PollVote> votos = new java.util.ArrayList<>();

}