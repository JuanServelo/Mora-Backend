package com.mora.meeting.controller;

import com.mora.meeting.security.AuthContext;
import com.mora.meeting.service.GoogleMeetOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/meetings/google")
@RequiredArgsConstructor
public class GoogleMeetController {
    private final GoogleMeetOAuthService oauth;
    private static final String COOKIE = "meeting_oauth_state";

    @GetMapping("/status")
    @Operation(summary = "Verificar conexão Google Meet", description = "Estado da configuração e autorização do condomínio. Não retorna tokens nem credenciais.")
    public GoogleMeetOAuthService.Status status() {
        AuthContext.requireManager();
        return oauth.status(AuthContext.tenant());
    }

    @PostMapping("/authorize")
    @Operation(summary = "Conectar Google Meet", description = "Inicia autorização offline com state e PKCE para o condomínio autenticado. Somente administração.")
    public ResponseEntity<Map<String, String>> authorize() {
        AuthContext.requireManager();
        var auth = oauth.authorize(AuthContext.tenant());
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.SET_COOKIE, cookie(auth.state(), Duration.ofMinutes(10)).toString())
                .body(Map.of("authorizationUrl", auth.url()));
    }

    @GetMapping("/callback")
    @Operation(summary = "Retorno OAuth Google", description = "Valida state de uso único e cookie do navegador, troca o código e persiste o token no servidor.")
    public ResponseEntity<Void> callback(@RequestParam(required = false) String state,
            @RequestParam(required = false) String code, @RequestParam(required = false) String error,
            @CookieValue(name = COOKIE, required = false) String browserState) {
        String result = "connected";
        try { oauth.complete(state, browserState, code, error); }
        catch (ResponseStatusException e) { result = "error"; }
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("Referrer-Policy", "no-referrer")
                .location(URI.create(oauth.frontendUrl() + "?googleMeet=" + result)).build();
    }

    private ResponseCookie cookie(String value, Duration age) {
        return ResponseCookie.from(COOKIE, value).httpOnly(true).secure(oauth.secureCookie())
                .sameSite("Lax").path("/api/meetings/google").maxAge(age).build();
    }
}
