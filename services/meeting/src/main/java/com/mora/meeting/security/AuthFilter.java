package com.mora.meeting.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private static final Set<String> READERS = Set.of("ADMIN_GERAL", "ADMIN_SINDICO", "MORADOR", "DONO_ALUGUEL");

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return "OPTIONS".equals(request.getMethod()) || path.equals("/actuator/health")
                || path.startsWith("/actuator/health/") || path.equals("/swagger-ui.html")
                || path.startsWith("/swagger-ui/") || path.equals("/api-docs") || path.startsWith("/api-docs/")
                || path.equals("/api/meetings/v3/api-docs") || path.startsWith("/api/meetings/v3/api-docs/")
                || path.equals("/api/meetings/swagger-ui.html") || path.startsWith("/api/meetings/swagger-ui/")
                || ("GET".equals(request.getMethod()) && path.equals("/api/meetings/google/callback"));
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        AuthContext.clear();
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) { deny(response, 401); return; }
        AuthContext.Actor actor;
        try { actor = jwtUtil.parse(header.substring(7)); }
        catch (Exception e) { deny(response, 401); return; }
        if (actor.perfil() == null || !READERS.contains(actor.perfil())
                || actor.condominioId() == null || actor.condominioId().isBlank()) { deny(response, 403); return; }
        AuthContext.set(actor);
        try {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            boolean selfAction = path.matches("/api/meetings/\\d+/convidados/\\d+/(presenca|avaliacoes)");
            boolean vote = path.matches("/api/polls/\\d+/votes");
            if (!"GET".equals(request.getMethod()) && !selfAction && !vote && !AuthContext.isManager()) {
                deny(response, 403); return;
            }
            chain.doFilter(request, response);
        } finally { AuthContext.clear(); }
    }

    private void deny(HttpServletResponse response, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + (status == 401 ? "Token ausente, inválido ou expirado." : "Perfil ou condomínio não autorizado.") + "\"}");
    }
}
