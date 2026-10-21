package com.mora.meeting.repository;

import com.mora.meeting.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface PollRepository extends JpaRepository<Poll, Long> {
    java.util.Optional<Poll> findByIdAndCondominioId(Long id, String condominioId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Poll p WHERE p.id = :id AND p.condominioId = :tenant")
    java.util.Optional<Poll> lockByIdAndTenant(@Param("id") Long id, @Param("tenant") String tenant);

    @Query("SELECT DISTINCT p FROM Poll p JOIN p.votos v WHERE v.unidadeId IS NULL AND p.status = :status")
    List<Poll> findLegacyPolls(@Param("status") com.mora.meeting.enums.PollStatus status);
    List<Poll> findByCondominioId(String condominioId);
    List<Poll> findByStatusAndDataHoraFimBefore(com.mora.meeting.enums.PollStatus status, LocalDateTime dataHora);
}
