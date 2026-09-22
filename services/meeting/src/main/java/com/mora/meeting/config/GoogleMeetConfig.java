package com.mora.meeting.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.meet.v2.Meet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

@Configuration
public class GoogleMeetConfig {

    private static final String APPLICATION_NAME = "Mora Meeting API";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    // O Docker mapeou o volume ./tokens para /app/tokens
    private static final String TOKENS_DIRECTORY_PATH = "/app/tokens";

    private static final List<String> SCOPES = Collections.singletonList("https://www.googleapis.com/auth/meetings.space.created");

    @Bean
    public Meet meetService() throws Exception {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        InputStream in = GoogleMeetConfig.class.getResourceAsStream("/credentials.json");
        if (in == null) {
            throw new FileNotFoundException("Arquivo credentials.json não encontrado no classpath.");
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        // Tenta carregar as credenciais já salvas. NÃO usa o LocalServerReceiver para não travar o Docker.
        Credential credential = flow.loadCredential("user");
        if (credential == null) {
            System.err.println("#################################################################");
            System.err.println("ERRO CRÍTICO: Token do Google Meet não encontrado em " + TOKENS_DIRECTORY_PATH);
            System.err.println("Você precisa rodar a classe GoogleMeetAuthHelper LOCALMENTE primeiro.");
            System.err.println("#################################################################");
            // Retorna nulo ou pode lançar exceção. Aqui apenas imprimimos o erro, 
            // e os métodos que usarem a API falharão (ou podemos retornar um mock).
        }

        return new Meet.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}