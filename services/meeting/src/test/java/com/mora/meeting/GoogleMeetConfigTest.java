package com.mora.meeting;

import com.mora.meeting.config.GoogleMeetConfig;
import com.mora.meeting.service.GoogleMeetOAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class GoogleMeetConfigTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner().withUserConfiguration(GoogleMeetConfig.class);

    @Test void disabledIntegrationStartsWithoutCredentialsAndFailsExplicitlyOnUse() {
        context.withPropertyValues("google.meet.enabled=false").run(app -> {
            assertThat(app).hasNotFailed();
            assertThatThrownBy(() -> app.getBean(GoogleMeetOAuthService.class).createLink("tenant-a"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(503));
        });
    }

    @Test void missingExternalFileDoesNotPreventBootAndEnabledIntegrationFailsExplicitlyOnUse() {
        context.withPropertyValues("google.meet.enabled=true", "google.meet.credentials-path=target/nonexistent-credentials.json",
                "google.meet.tokens-path=target/test-tokens").run(app -> {
            assertThat(app).hasNotFailed();
            assertThatThrownBy(() -> app.getBean(GoogleMeetOAuthService.class).createLink("tenant-a"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(503));
        });
    }
}
