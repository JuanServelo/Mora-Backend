package com.mora.meeting;

import com.mora.meeting.service.GoogleMeetOAuthService;
import com.google.api.client.testing.http.*;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleMeetOAuthServiceTest {
    @TempDir Path dir;
    Clock clock;
    final AtomicReference<String> response = new AtomicReference<>();
    final AtomicReference<String> requestBody = new AtomicReference<>();
    final AtomicInteger tokenRequests = new AtomicInteger();
    int tokenStatus = 200;
    int meetStatus = 200;
    final MockHttpTransport transport = new MockHttpTransport() {
        @Override public LowLevelHttpRequest buildRequest(String method, String url) {
            return new MockLowLevelHttpRequest(url) {
                @Override public LowLevelHttpResponse execute() throws IOException {
                    if (url.contains("/token")) {
                        tokenRequests.incrementAndGet();
                        requestBody.set(getContentAsString());
                        return new MockLowLevelHttpResponse().setContentType("application/json")
                                .setStatusCode(tokenStatus).setContent(response.get());
                    }
                    return new MockLowLevelHttpResponse().setContentType("application/json")
                            .setStatusCode(meetStatus)
                            .setContent("{\"meetingUri\":\"https://meet.google.com/test-room\"}");
                }
            };
        }
    };

    @BeforeEach void setup() throws Exception {
        clock = mock(Clock.class);
        when(clock.instant()).thenReturn(Instant.parse("2026-10-25T12:00:00Z"));
        response.set("{\"access_token\":\"access\",\"refresh_token\":\"refresh\",\"expires_in\":3600,\"token_type\":\"Bearer\"}");
        Files.writeString(dir.resolve("credentials.json"), "{\"web\":{\"client_id\":\"test\",\"client_secret\":\"secret\","
                + "\"auth_uri\":\"https://accounts.google.com/o/oauth2/auth\",\"token_uri\":\"https://oauth2.googleapis.com/token\"}}");
    }
    GoogleMeetOAuthService service() {
        return new GoogleMeetOAuthService(true, dir.resolve("credentials.json").toString(), dir.resolve("tokens").toString(),
                "http://localhost:8091/api/meetings/google/callback", "http://localhost:5173/adm/reunioes", transport, clock);
    }

    @Test void persistsAuthorizationAcrossRestartsWithoutSharingItWithAnotherTenant() {
        var oauth = service();
        assertThat(oauth.status("tenant-a").configured()).isTrue();
        assertThat(oauth.status("tenant-a").connected()).isFalse();
        var auth = oauth.authorize("tenant-a");
        assertThat(auth.url()).contains("access_type=offline", "prompt=consent", "code_challenge=", "code_challenge_method=S256");
        oauth.complete(auth.state(), auth.state(), "code", null);
        assertThat(requestBody.get()).contains("code_verifier=", "code=code");
        assertThat(service().status("tenant-a").connected()).isTrue();
        assertThat(service().status("tenant-b").connected()).isFalse();
        assertThat(service().status("../../tenant-a").connected()).isFalse();
        assertThatThrownBy(() -> service().createLink("tenant-b")).isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(409));
    }

    @Test void rejectsWrongBrowserReplaysAndExpiredStateBeforeContactingGoogle() {
        var oauth = service();
        var auth = oauth.authorize("tenant-a");
        assertThatThrownBy(() -> oauth.complete(auth.state(), "other-browser", "code", null)).isInstanceOf(ResponseStatusException.class);
        assertThat(requestBody.get()).isNull();
        oauth.complete(auth.state(), auth.state(), "code", null);
        requestBody.set(null);
        assertThatThrownBy(() -> oauth.complete(auth.state(), auth.state(), "code", null)).isInstanceOf(ResponseStatusException.class);
        var expired = oauth.authorize("tenant-a");
        when(clock.instant()).thenReturn(Instant.parse("2026-10-25T12:11:00Z"));
        assertThatThrownBy(() -> oauth.complete(expired.state(), expired.state(), "code", null)).isInstanceOf(ResponseStatusException.class);
        assertThat(requestBody.get()).isNull();
    }

    @Test void denialOrMissingRefreshTokenDoesNotOverwriteWorkingCredentials() {
        var oauth = service();
        var auth = oauth.authorize("tenant-a");
        oauth.complete(auth.state(), auth.state(), "code", null);
        var denied = oauth.authorize("tenant-a");
        assertThatThrownBy(() -> oauth.complete(denied.state(), denied.state(), null, "access_denied"))
                .isInstanceOf(ResponseStatusException.class);
        response.set("{\"access_token\":\"incomplete\",\"token_type\":\"Bearer\"}");
        var incomplete = oauth.authorize("tenant-a");
        assertThatThrownBy(() -> oauth.complete(incomplete.state(), incomplete.state(), "code", null))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(service().status("tenant-a").connected()).isTrue();
    }

    @Test void missingOrInvalidCredentialsReturnSafeErrorsAndRecoverWithoutRestart() throws Exception {
        var oauth = service();
        var original = Files.readString(dir.resolve("credentials.json"));
        Files.delete(dir.resolve("credentials.json"));
        assertThat(oauth.status("tenant-a").configured()).isFalse();
        assertThatThrownBy(() -> oauth.createLink("tenant-a")).isInstanceOf(ResponseStatusException.class);
        Files.writeString(dir.resolve("credentials.json"), "not-json");
        assertThat(oauth.status("tenant-a").configured()).isFalse();
        assertThatThrownBy(() -> oauth.createLink("tenant-a")).isInstanceOf(ResponseStatusException.class);
        Files.writeString(dir.resolve("credentials.json"), original);
        assertThat(oauth.status("tenant-a").configured()).isTrue();
    }

    @Test void refreshesExpiredAccessAndPersistsItForTheNextApplicationInstance() {
        response.set("{\"access_token\":\"old\",\"refresh_token\":\"refresh\",\"expires_in\":-1,\"token_type\":\"Bearer\"}");
        var oauth = service();
        var auth = oauth.authorize("tenant-a");
        oauth.complete(auth.state(), auth.state(), "code", null);
        response.set("{\"access_token\":\"renewed\",\"expires_in\":3600,\"token_type\":\"Bearer\"}");
        assertThat(oauth.createLink("tenant-a")).isEqualTo("https://meet.google.com/test-room");
        assertThat(requestBody.get()).contains("grant_type=refresh_token");
        assertThat(service().createLink("tenant-a")).isEqualTo("https://meet.google.com/test-room");
        assertThat(tokenRequests.get()).isEqualTo(2);
    }

    @Test void revokedRefreshTokenRequiresANewAuthorization() {
        response.set("{\"access_token\":\"old\",\"refresh_token\":\"refresh\",\"expires_in\":-1,\"token_type\":\"Bearer\"}");
        var oauth = service();
        var auth = oauth.authorize("tenant-a");
        oauth.complete(auth.state(), auth.state(), "code", null);
        response.set("{\"error\":\"invalid_grant\"}");
        tokenStatus = 400;
        assertThatThrownBy(() -> oauth.createLink("tenant-a")).isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(409));
        assertThat(service().status("tenant-a").connected()).isFalse();
    }

    @Test void revocationBeforeAccessTokenExpiryAlsoAllowsReauthorization() {
        var oauth = service();
        var auth = oauth.authorize("tenant-a");
        oauth.complete(auth.state(), auth.state(), "code", null);
        meetStatus = 401;
        tokenStatus = 400;
        response.set("{\"error\":\"invalid_grant\"}");
        assertThatThrownBy(() -> oauth.createLink("tenant-a")).isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(409));
        assertThat(service().status("tenant-a").connected()).isFalse();
    }
}
