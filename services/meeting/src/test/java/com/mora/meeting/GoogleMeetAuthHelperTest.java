package com.mora.meeting;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class GoogleMeetAuthHelperTest {
    @Test void requestsExplicitConsentSoGoogleCanIssueAnOfflineRefreshToken() {
        var url = new GoogleAuthorizationCodeRequestUrl("client", "http://localhost:8888/Callback",
                List.of("https://www.googleapis.com/auth/meetings.space.created")).setAccessType("offline");
        GoogleMeetAuthHelper.requestOfflineConsent(url);
        assertThat(url.get("prompt")).isEqualTo("consent");
        assertThat(url.getAccessType()).isEqualTo("offline");
    }
}
