package com.mora.meeting.repository;

import com.mora.meeting.entity.Ata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AtaRepository extends JpaRepository<Ata, Long> {

    boolean existsByMeetingId(Long meetingId);

    Optional<Ata> findByMeetingId(Long meetingId);
}