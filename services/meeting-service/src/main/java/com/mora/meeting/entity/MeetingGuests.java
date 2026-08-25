package com.mora.meeting.entity;

import com.mora.meeting.enums.AttendanceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeetingGuests{

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_presenca", nullable = false)
    private AttendanceStatus status;

    @Column(name = "nota_avaliacao")
    private Integer nota;

    @Column(name = "comentario_avaliacao", length = 500)
    private String comentario;

    public MeetingGuests(Long usuarioId, AttendanceStatus status) {
        this.usuarioId = usuarioId;
        this.status = status;
    }
}