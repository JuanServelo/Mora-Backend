package com.mora.meeting.config;

import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.util.store.FileDataStoreFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import java.io.*;
import com.mora.meeting.service.GoogleMeetOAuthService;
import java.time.Clock;

@Configuration
public class GoogleMeetConfig {
    @Bean
    public GoogleMeetOAuthService googleMeetOAuthService(
            @Value("${google.meet.enabled:true}") boolean enabled,
            @Value("${google.meet.credentials-path:}") String credentialsPath,
            @Value("${google.meet.tokens-path:tokens}") String tokensPath,
            @Value("${google.meet.redirect-uri:http://localhost:8087/api/meetings/google/callback}") String redirectUri,
            @Value("${google.meet.frontend-url:http://localhost:5173/adm/reunioes}") String frontendUrl) throws Exception {
        return new GoogleMeetOAuthService(enabled, credentialsPath, tokensPath,
                redirectUri, frontendUrl, GoogleNetHttpTransport.newTrustedTransport(), Clock.systemUTC());
    }

    /** Only the OAuth helper writes the shared store. Refreshes stay in memory so an
     * old application instance cannot overwrite a newly authorized refresh token. */
    static Credential loadCredential(HttpTransport transport, JsonFactory json,
                                     GoogleClientSecrets secrets, String tokensPath) throws IOException {
        var store = new FileDataStoreFactory(new File(tokensPath))
                .<StoredCredential>getDataStore(StoredCredential.DEFAULT_DATA_STORE_ID);
        var stored = store.get("user");
        if (stored == null || stored.getRefreshToken() == null) {
            throw new IOException("Token Google Meet ausente no diretório externo.");
        }
        var details = secrets.getDetails();
        return new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                .setTransport(transport).setJsonFactory(json)
                .setTokenServerEncodedUrl(details.getTokenUri())
                .setClientAuthentication(new ClientParametersAuthentication(details.getClientId(), details.getClientSecret()))
                .build().setAccessToken(stored.getAccessToken()).setRefreshToken(stored.getRefreshToken())
                .setExpirationTimeMilliseconds(stored.getExpirationTimeMilliseconds());
    }
}
