package com.mora.meeting;

import com.google.api.services.meet.v2.Meet;
import com.mora.meeting.config.GoogleMeetConfig;
import com.mora.meeting.service.MeetLinkService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class GoogleMeetConfigTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner().withUserConfiguration(GoogleMeetConfig.class);

    @Test void disabledIntegrationStartsWithoutCredentialsAndCreatesNoFakeLink() {
        context.withPropertyValues("google.meet.enabled=false").run(app -> {
            assertThat(app).hasNotFailed();
            assertThat(new MeetLinkService(app.getBeanProvider(Meet.class)).createLink()).isNull();
        });
    }

    @Test void missingExternalFileDoesNotPreventBootAndEnabledIntegrationFailsExplicitlyOnUse() {
        context.withPropertyValues("google.meet.enabled=true", "google.meet.credentials-path=target/nonexistent-credentials.json",
                "google.meet.tokens-path=target/test-tokens").run(app -> {
            assertThat(app).hasNotFailed();
            assertThatThrownBy(() -> new MeetLinkService(app.getBeanProvider(Meet.class)).createLink())
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(503));
        });
    }
}
