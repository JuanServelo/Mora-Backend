package com.mora.meeting.repository;

import com.mora.meeting.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface PollRepository extends JpaRepository<Poll, Long> {
    List<Poll> findByMeetingId(Long meetingId);
    
    @Query("SELECT p FROM Poll p WHERE p.meeting.dataHoraInicio >= :start AND p.meeting.dataHoraInicio <= :end")
    List<Poll> findByMeetingDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}