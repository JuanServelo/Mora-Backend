package com.mora.meeting.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.meet.v2.Meet;
import com.google.api.services.meet.v2.model.Space;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;

/** Lazy OAuth configuration and persistent, tenant-isolated credentials. No browser or shell runs on the server. */
public class GoogleMeetOAuthService {
    public record Status(boolean configured, boolean connected, String message) {}
    public record Authorization(String url, String state) {}
    private record Pending(String tenant, String verifier, Instant expires) {}
    private static final String SCOPE = "https://www.googleapis.com/auth/meetings.space.created";
    private final boolean enabled;
    private final Path credentials;
    private final Path tokens;
    private final String redirectUri;
    private final String frontendUrl;
    private final HttpTransport transport;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Pending> pending = new HashMap<>();

    public GoogleMeetOAuthService(boolean enabled, String credentials, String tokens, String redirectUri,
                                  String frontendUrl, HttpTransport transport, Clock clock) {
        this.enabled = enabled;
        this.credentials = Path.of(credentials);
        this.tokens = Path.of(tokens);
        this.redirectUri = redirectUri;
        this.frontendUrl = frontendUrl;
        this.transport = transport;
        this.clock = clock;
    }

    public synchronized Status status(String tenant) {
        try {
            var credential = flow(tenant).loadCredential("user");
            boolean connected = credential != null && credential.getRefreshToken() != null
                    && !credential.getRefreshToken().isBlank();
            return new Status(true, connected, connected ? "Google Meet conectado." : "Conecte sua conta Google para criar reuniões.");
        } catch (Exception e) {
            return new Status(false, false, "Google Meet indisponível. Configure as credenciais externas para conectar sua conta.");
        }
    }

    public synchronized Authorization authorize(String tenant) {
        try {
            var flow = flow(tenant);
            pending.entrySet().removeIf(e -> !e.getValue().expires().isAfter(clock.instant()));
            if (pending.size() >= 1000) throw unavailable();
            String state = nonce();
            String verifier = nonce();
            var url = flow.newAuthorizationUrl().setRedirectUri(redirectUri).setState(state);
            url.set("prompt", "consent");
            url.set("code_challenge", Base64.getUrlEncoder().withoutPadding().encodeToString(hash(verifier)));
            url.set("code_challenge_method", "S256");
            pending.put(state, new Pending(tenant, verifier, clock.instant().plus(Duration.ofMinutes(10))));
            return new Authorization(url.build(), state);
        } catch (IOException e) { throw unavailable(); }
    }

    /** Single use state is also bound to the initiating browser by an HttpOnly cookie. */
    public synchronized void complete(String state, String browserState, String code, String error) {
        if (state == null || browserState == null || !MessageDigest.isEqual(
                state.getBytes(StandardCharsets.UTF_8), browserState.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Autorização Google inválida. Inicie novamente pela tela de reuniões.");
        }
        Pending request = pending.remove(state);
        if (request == null || !request.expires().isAfter(clock.instant())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Autorização Google expirada ou já utilizada.");
        }
        if (error != null || code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A autorização do Google não foi concluída.");
        }
        try {
            var flow = flow(request.tenant());
            var exchange = flow.newTokenRequest(code).setRedirectUri(redirectUri);
            exchange.set("code_verifier", request.verifier());
            var response = exchange.execute();
            // Never overwrite a working authorization with an incomplete grant.
            if (response.getRefreshToken() == null || response.getRefreshToken().isBlank()) throw unavailable();
            flow.createAndStoreCredential(response, "user");
        } catch (IOException e) { throw unavailable(); }
    }

    public synchronized String createLink(String tenant) {
        try {
            var flow = flow(tenant);
            Credential credential = flow.loadCredential("user");
            if (credential == null || credential.getRefreshToken() == null || credential.getRefreshToken().isBlank()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Conecte sua conta Google na seção de reuniões antes de criar uma reunião.");
            }
            var meet = new Meet.Builder(transport, GsonFactory.getDefaultInstance(), request -> {
                credential.initialize(request);
                request.setConnectTimeout(5000);
                request.setReadTimeout(10000);
            }).setApplicationName("Mora Meeting API").build();
            return meet.spaces().create(new Space()).execute().getMeetingUri();
        } catch (TokenResponseException e) {
            if (e.getDetails() != null && "invalid_grant".equals(e.getDetails().getError())) {
                try { flow(tenant).getCredentialDataStore().delete("user"); } catch (IOException ignored) { }
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A autorização Google expirou. Conecte sua conta novamente.");
            }
            throw unavailable();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 401) {
                try { flow(tenant).getCredentialDataStore().delete("user"); } catch (IOException ignored) { }
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A autorização Google expirou. Conecte sua conta novamente.");
            }
            throw unavailable();
        } catch (IOException e) { throw unavailable(); }
    }

    private GoogleAuthorizationCodeFlow flow(String tenant) throws IOException {
        if (!enabled || !Files.isRegularFile(credentials) || tenant == null || tenant.isBlank()) throw unavailable();
        GoogleClientSecrets secrets;
        try (var reader = Files.newBufferedReader(credentials, StandardCharsets.UTF_8)) {
            secrets = GoogleClientSecrets.load(GsonFactory.getDefaultInstance(), reader);
            var details = secrets.getDetails();
            if (details.getClientId() == null || details.getClientId().isBlank()
                    || details.getClientSecret() == null || details.getClientSecret().isBlank()) {
                throw new IOException("Configuração OAuth incompleta.");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new IOException("Configuração OAuth inválida.", e);
        }
        // Hash prevents directory traversal and keeps tenant identifiers out of filenames.
        String directory = HexFormat.of().formatHex(hash(tenant));
        return new GoogleAuthorizationCodeFlow.Builder(transport, GsonFactory.getDefaultInstance(), secrets, List.of(SCOPE))
                .setDataStoreFactory(new FileDataStoreFactory(tokens.resolve(directory).toFile()))
                .setAccessType("offline")
                .setRequestInitializer(request -> { request.setConnectTimeout(5000); request.setReadTimeout(10000); })
                .build();
    }

    private String nonce() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] hash(String value) {
        try { return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    public String frontendUrl() { return frontendUrl; }
    public boolean secureCookie() { return redirectUri.startsWith("https://"); }
    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Google Meet indisponível. Verifique as credenciais externas e conecte sua conta na seção de reuniões.");
    }
}
