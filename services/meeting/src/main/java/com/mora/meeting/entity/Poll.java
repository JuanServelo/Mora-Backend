package com.mora.meeting.entity;

import com.mora.meeting.enums.PollStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Poll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private String descricao;

    /** Cliente dono deste registro. Todo dado de domínio pertence a um condomínio. */
    @Column(name = "condominio_id")
    private String condominioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    private PollStatus status;

    @Builder.Default
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PollOption> opcoes = new ArrayList<>();

    public void addOption(String descricao) {
        PollOption option = new PollOption();
        option.setDescricao(descricao);
        option.setPoll(this);
        this.opcoes.add(option);
    }
}