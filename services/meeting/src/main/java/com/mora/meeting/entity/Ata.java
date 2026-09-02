package com.mora.meeting.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tb_atas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "meeting_id", nullable = false, unique = true)
    private Meeting meeting;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String topicosDiscutidos;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String decisoesTomadas;

    @ElementCollection
    @CollectionTable(name = "tb_ata_presentes", joinColumns = @JoinColumn(name = "ata_id"))
    @Column(name = "morador_id")
    private List<Long> idPresentes;

    @Column(nullable = false)
    private LocalDateTime dataPublicacao;

    /** Cliente dono deste registro. Todo dado de domínio pertence a um condomínio. */
    @Column(name = "condominio_id")
    private String condominioId;
}