package com.mora.meeting.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {
    private final SecretKey key;
    public JwtUtil(@Value("${jwt.secret}") String secret) {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    public AuthContext.Actor parse(String token) {
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Long id = Long.valueOf(String.valueOf(claims.get("id")));
        if (id <= 0) throw new IllegalArgumentException("ID inválido");
        return new AuthContext.Actor(id, claims.get("perfil", String.class), claims.get("condominioId", String.class), token);
    }
}
