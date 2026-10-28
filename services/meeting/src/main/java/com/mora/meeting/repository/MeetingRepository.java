package com.mora.meeting.repository;

import com.mora.meeting.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    java.util.Optional<Meeting> findByIdAndCondominioId(Long id, String condominioId);
    List<Meeting> findByCondominioId(String condominioId);

    @Query("SELECT DISTINCT m FROM Meeting m LEFT JOIN m.convidados c WHERE m.condominioId = :condominioId AND (m.idOrganizador = :usuarioId OR c.usuarioId = :usuarioId)")
    List<Meeting> findByOrganizadorOrConvidado(@Param("condominioId") String condominioId, @Param("usuarioId") Long usuarioId);
}
