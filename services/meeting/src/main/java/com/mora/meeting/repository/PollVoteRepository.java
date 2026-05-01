package com.mora.meeting.repository;

import com.mora.meeting.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    // Retorna true se já existir um registro com esse usuário nessa votação
    boolean existsByPollIdAndUsuarioId(Long pollId, Long usuarioId);
}