package com.mora.meeting;

import com.google.api.client.auth.oauth2.Credential;
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

            InputStream in = GoogleMeetAuthHelper.class.getResourceAsStream("/credentials.json");
            if (in == null) {
                throw new FileNotFoundException("Arquivo credentials.json não encontrado na pasta resources.");
            }

            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                    .setAccessType("offline")
                    .build();

            // Abre o navegador local na porta 8888 (para não conflitar com o Traefik na 8080)
            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

            System.out.println("Autenticação bem sucedida!");
            System.out.println("Pasta gerada em: " + TOKENS_DIRECTORY_PATH);
            System.out.println("Agora você já pode reiniciar os containers do Docker.");
            
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erro durante a autenticação. Verifique se a porta 8888 não está ocupada.");
        }
    }
}
