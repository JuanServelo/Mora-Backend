package com.mora.meeting.entity;

import com.mora.meeting.enums.MeetingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tb_meetings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false)
    private LocalDateTime dataHoraInicio;

    @Column(nullable = false)
    private LocalDateTime dataHoraFim;

    @Column(nullable = false)
    private Long idOrganizador;

    @ElementCollection
    @CollectionTable(name = "tb_meeting_convidados", joinColumns = @JoinColumn(name = "meeting_id"))
    @Column(name = "convidado_id")
    private List<Long> idConvidados;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MeetingStatus status;

    // --- Integração com o Google Meet ---

    @Column(length = 255)
    private String meetLink;

    @Column(unique = true)
    private String googleEventId;
}