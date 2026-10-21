package com.mora.meeting.config;

import com.mora.meeting.enums.PollStatus;
import com.mora.meeting.repository.PollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Keep historical votes intact; do not mix unverifiable user votes with new unit votes. */
@Component
@RequiredArgsConstructor
public class LegacyVoteMigration implements ApplicationRunner {
    private final PollRepository polls;
    @Override @Transactional
    public void run(ApplicationArguments args) {
        var legacy = polls.findLegacyPolls(PollStatus.ABERTA);
        legacy.forEach(p -> p.setStatus(PollStatus.ENCERRADA));
        polls.saveAll(legacy);
    }
}
