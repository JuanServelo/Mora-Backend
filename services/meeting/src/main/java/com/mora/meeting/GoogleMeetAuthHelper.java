package com.mora.meeting;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Utilitário para rodar LOCALMENTE e gerar a pasta "tokens" 
 * antes de subir a aplicação no Docker.
 */
public class GoogleMeetAuthHelper {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    // O token será salvo na raiz do repositório backend, em "tokens"
    private static final String TOKENS_DIRECTORY_PATH = Paths.get("..", "..", "tokens").toAbsolutePath().normalize().toString();
    private static final List<String> SCOPES = Collections.singletonList("https://www.googleapis.com/auth/meetings.space.created");

    public static void main(String[] args) {
        try {
            System.out.println("Iniciando fluxo de autenticação...");
            System.out.println("Os tokens serão salvos em: " + TOKENS_DIRECTORY_PATH);

            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            boolean reauthorize = java.util.Arrays.asList(args).contains("--reauthorize");
            String credentialsPath = java.util.Arrays.stream(args).filter(arg -> !arg.equals("--reauthorize"))
                    .findFirst().orElse(System.getenv("GOOGLE_MEET_CREDENTIALS_PATH"));
            if (credentialsPath == null || credentialsPath.isBlank()) {
                throw new FileNotFoundException("Informe o caminho externo como argumento ou GOOGLE_MEET_CREDENTIALS_PATH.");
            }
            GoogleClientSecrets clientSecrets;
            try (var reader = java.nio.file.Files.newBufferedReader(Paths.get(credentialsPath), java.nio.charset.StandardCharsets.UTF_8)) {
                clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, reader);
            }

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                    .setAccessType("offline")
                    .build();

            if (reauthorize) {
                var tokenFile = Paths.get(TOKENS_DIRECTORY_PATH, "StoredCredential");
                if (java.nio.file.Files.exists(tokenFile)) {
                    java.nio.file.Files.copy(tokenFile, tokenFile.resolveSibling("StoredCredential.backup-" + System.nanoTime()));
                }
                flow.getCredentialDataStore().delete("user");
            }

            // Abre o navegador local na porta 8888 (para não conflitar com o Traefik na 8080)
            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver) {
                @Override
                protected void onAuthorization(AuthorizationCodeRequestUrl url) throws java.io.IOException {
                    requestOfflineConsent(url);
                    super.onAuthorization(url);
                }
            }.authorize("user");

            if (credential.getRefreshToken() == null || credential.getRefreshToken().isBlank()) {
                throw new java.io.IOException("O Google não retornou refresh token. Execute com --reauthorize e conclua o consentimento.");
            }

            System.out.println("Autenticação bem sucedida!");
            System.out.println("Pasta gerada em: " + TOKENS_DIRECTORY_PATH);
            System.out.println("Agora você já pode reiniciar os containers do Docker.");
            
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erro durante a autenticação. Verifique se a porta 8888 não está ocupada.");
        }
    }

    static void requestOfflineConsent(AuthorizationCodeRequestUrl url) {
        url.set("prompt", "consent");
    }
}
