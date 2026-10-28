package com.mora.meeting.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Component
public class AuthApiClient {
    private final String base;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    public AuthApiClient(@Value("${AUTH_API_URL:http://localhost:3001}") String base, ObjectMapper mapper) {
        this.base = base.replaceAll("/+$", "");
        this.mapper = mapper;
    }

    /** Resolves the current unit from the authority, never from the vote body or an unverified claim. */
    public String currentUnit() {
        JsonNode user = get("/api/auth/me").path("usuario");
        var actor = AuthContext.get();
        if (!String.valueOf(actor.usuarioId()).equals(user.path("id").asText())
                || !AuthContext.tenant().equals(user.path("condominioId").asText())
                || !actor.perfil().equals(user.path("perfil").asText())
                || !"active".equals(user.path("status").asText())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vínculo do usuário não corresponde à sessão.");
        }
        String unit = user.path("unidadeId").asText(null);
        return unit == null || unit.isBlank() ? null : unit;
    }

    public void validateGuests(List<Long> ids) {
        JsonNode users = get("/api/user-management/users?condominioId="
                + URLEncoder.encode(AuthContext.tenant(), StandardCharsets.UTF_8)).path("usuarios");
        if (!users.isArray()) throw unavailable();
        Set<Long> permitted = new HashSet<>();
        for (JsonNode user : users) {
            if (AuthContext.tenant().equals(user.path("condominioId").asText())
                    && "active".equals(user.path("status").asText())) permitted.add(user.path("id").asLong());
        }
        if (ids.stream().anyMatch(id -> id == null || !permitted.contains(id))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Convidados devem ser usuários ativos do mesmo condomínio.");
        }
    }

    private JsonNode get(String path) {
        try {
            var request = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(3))
                    .header("Authorization", "Bearer " + AuthContext.get().token()).GET().build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new ResponseStatusException(HttpStatus.valueOf(response.statusCode()), "Sessão não autorizada pelo auth-api.");
            }
            if (response.statusCode() != 200) throw unavailable();
            return mapper.readTree(response.body());
        } catch (ResponseStatusException e) { throw e; }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw unavailable(); }
        catch (Exception e) { throw unavailable(); }
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível confirmar o vínculo no auth-api.");
    }
}
