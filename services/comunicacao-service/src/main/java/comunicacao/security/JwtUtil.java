package comunicacao.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {

    private final SecretKey secretKey;

    /** HS256 exige chave de pelo menos 256 bits. */
    static final int TAMANHO_MINIMO = 32;

    /**
     * Recusa subir sem segredo, em vez de subir inseguro.
     *
     * O padrão antigo era `changeme-insecure-default`: um deploy que esquecesse
     * a variável aceitaria tokens assinados com uma chave que está no repositório
     * — qualquer um poderia se passar por qualquer usuário. Falhar na subida é o
     * único jeito de o esquecimento aparecer antes de virar brecha.
     */
    public JwtUtil(@Value("${jwt.secret:}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET não definido. Configure a mesma chave do auth-api no docker/.env.");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < TAMANHO_MINIMO) {
            throw new IllegalStateException(
                    "JWT_SECRET curto demais: são necessários pelo menos " + TAMANHO_MINIMO + " caracteres.");
        }
        this.secretKey = Keys.hmacShaKeyFor(bytes);
    }

    public JwtClaims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String authUserId = String.valueOf(claims.get("id"));
        String email      = (String) claims.get("email");
        String perfil     = (String) claims.get("perfil");
        String condominioId = (String) claims.get("condominioId");
        String nome       = (String) claims.get("nome");
        return new JwtClaims(authUserId, email, perfil, condominioId, nome);
    }
}
