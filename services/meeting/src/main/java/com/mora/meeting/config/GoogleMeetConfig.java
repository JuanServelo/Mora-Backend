package com.mora.meeting.config;

import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.meet.v2.Meet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

@Configuration
public class GoogleMeetConfig {
    @Bean
    @Lazy
    @ConditionalOnProperty(name = "google.meet.enabled", havingValue = "true")
    public Meet meetService(@Value("${google.meet.credentials-path}") String credentialsPath,
                            @Value("${google.meet.tokens-path}") String tokensPath) throws Exception {
        var transport = GoogleNetHttpTransport.newTrustedTransport();
        var json = GsonFactory.getDefaultInstance();
        GoogleClientSecrets secrets;
        try (Reader reader = Files.newBufferedReader(Path.of(credentialsPath), StandardCharsets.UTF_8)) {
            secrets = GoogleClientSecrets.load(json, reader);
        }
        var flow = new GoogleAuthorizationCodeFlow.Builder(transport, json, secrets,
                List.of("https://www.googleapis.com/auth/meetings.space.created"))
                .setDataStoreFactory(new FileDataStoreFactory(new File(tokensPath))).setAccessType("offline").build();
        var credential = flow.loadCredential("user");
        if (credential == null) throw new IOException("Token Google Meet ausente no diretório externo.");
        return new Meet.Builder(transport, json, request -> {
            credential.initialize(request);
            request.setConnectTimeout(5000);
            request.setReadTimeout(10000);
        }).setApplicationName("Mora Meeting API").build();
    }
}
