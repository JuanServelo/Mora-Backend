package com.mora.meeting.config;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;

class GoogleMeetCredentialStoreTest {
    @TempDir Path directory;

    @Test void failedRefreshCannotRestoreOldTokenAfterReauthorization() throws Exception {
        exercise(false);
    }

    @Test void successfulRefreshCannotOverwriteNewAuthorizationEither() throws Exception {
        exercise(true);
    }

    private void exercise(boolean success) throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/token", exchange -> {
            byte[] body = (success ? "{\"access_token\":\"refreshed-access\",\"expires_in\":3600,\"token_type\":\"Bearer\"}"
                    : "{\"error\":\"invalid_grant\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(success ? 200 : 400, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            var store = new FileDataStoreFactory(directory.toFile())
                    .<StoredCredential>getDataStore(StoredCredential.DEFAULT_DATA_STORE_ID);
            store.set("user", new StoredCredential().setRefreshToken("old-token").setAccessToken("old-access"));
            var details = new GoogleClientSecrets.Details().setClientId("test-client").setClientSecret("test-secret")
                    .setTokenUri("http://127.0.0.1:" + server.getAddress().getPort() + "/token");
            var secrets = new GoogleClientSecrets().setWeb(details);
            var credential = GoogleMeetConfig.loadCredential(new NetHttpTransport(), GsonFactory.getDefaultInstance(),
                    secrets, directory.toString());
            store.set("user", new StoredCredential().setRefreshToken("new-authorization").setAccessToken("new-access"));
            byte[] before = Files.readAllBytes(directory.resolve(StoredCredential.DEFAULT_DATA_STORE_ID));
            if (success) assertThat(credential.refreshToken()).isTrue();
            else assertThatThrownBy(credential::refreshToken).isInstanceOf(TokenResponseException.class);
            assertThat(Files.readAllBytes(directory.resolve(StoredCredential.DEFAULT_DATA_STORE_ID))).isEqualTo(before);
            var reread = new FileDataStoreFactory(directory.toFile())
                    .<StoredCredential>getDataStore(StoredCredential.DEFAULT_DATA_STORE_ID).get("user");
            assertThat(reread.getRefreshToken()).isEqualTo("new-authorization");
        } finally { server.stop(0); }
    }
}
