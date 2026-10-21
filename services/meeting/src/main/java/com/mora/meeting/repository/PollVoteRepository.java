package com.mora.meeting.repository;

import com.mora.meeting.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    boolean existsByPollIdAndUnidadeId(Long pollId, String unidadeId);
    boolean existsByPollIdAndUnidadeIdIsNull(Long pollId);
}
