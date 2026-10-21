package portaria.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    /**
     * Rotas sem JWT: health check e a documentação da API. O Swagger só
     * descreve os endpoints; cada chamada feita por ele continua exigindo token.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String caminho = request.getServletPath();
        return caminho.startsWith("/actuator")
                || caminho.startsWith("/swagger-ui")
                || caminho.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        AuthContext.clear();
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            escrever401(response, "Token de autenticação não fornecido.");
            return;
        }
        String token = header.substring(7);
        try {
            AuthContext.set(jwtUtil.parse(token));
            AuthContext.setToken(token);
        } catch (Exception e) {
            escrever401(response, "Token inválido ou expirado.");
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            AuthContext.clear();
        }
    }

    private void escrever401(HttpServletResponse response, String mensagem) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"sucesso\":false,\"mensagem\":\"" + mensagem + "\"}");
    }
}
