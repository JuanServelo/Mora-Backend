package com.mora.plan.security;

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
     * Rotas sem JWT: health check, preflight de CORS e a documentação da API.
     * O Swagger só descreve os endpoints; cada chamada feita por ele continua
     * exigindo token.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String caminho = request.getRequestURI().substring(request.getContextPath().length());
        return caminho.startsWith("/actuator")
                || caminho.startsWith("/api/plans/v3/api-docs")
                || caminho.startsWith("/api/plans/swagger-ui")
                || caminho.equals("/swagger-ui.html")
                || caminho.startsWith("/swagger-ui/")
                || caminho.startsWith("/v3/api-docs")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
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
        } catch (Exception e) {
            escrever401(response, "Token inválido ou expirado.");
            return;
        }
        try {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            boolean geral = "ADMIN_GERAL".equals(AuthContext.get().perfil());
            boolean leitura = "GET".equals(request.getMethod());
            boolean assinatura = path.startsWith("/api/assinaturas") || path.startsWith("/api/plans/subscriptions");
            String marcador = "/condominio/";
            int inicio = path.indexOf(marcador);
            boolean proprio = inicio >= 0 && path.substring(inicio + marcador.length()).split("/")[0]
                    .equals(AuthContext.get().condominioId());
            if (!geral && (!leitura || (assinatura && !proprio))) {
                response.setStatus(403);
                return;
            }
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
