package com.mora.meeting.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

/** Checks the verified token's tenant with plan-service, including direct calls to this port. */
@Component
public class PlanModuleFilter extends OncePerRequestFilter {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final String planUrl;

    public PlanModuleFilter(@Value("${PLAN_SERVICE_URL:http://localhost:8093}") String planUrl) {
        this.planUrl = planUrl.replaceAll("/+$", "");
    }

    static String module(String path) {
        if (matches(path, "/api/meetings")) return "reunioes";
        if (matches(path, "/api/polls")) return "votacoes";
        return null;
    }

    private static boolean matches(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod()) || module(path(request)) == null
                || path(request).equals("/api/meetings/v3/api-docs") || path(request).startsWith("/api/meetings/v3/api-docs/")
                || path(request).equals("/api/meetings/swagger-ui.html") || path(request).startsWith("/api/meetings/swagger-ui/")
                || ("GET".equals(request.getMethod()) && path(request).equals("/api/meetings/google/callback"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            deny(response, 401);
            return;
        }
        int status;
        try {
            var check = HttpRequest.newBuilder(URI.create(planUrl + "/api/plans/access/" + module(path(request))))
                    .timeout(Duration.ofSeconds(3)).header("Authorization", token).GET().build();
            status = client.send(check, HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            deny(response, 503);
            return;
        } catch (IOException | IllegalArgumentException e) {
            deny(response, 503);
            return;
        }
        if (status != 204) {
            deny(response, status == 401 || status == 403 ? status : 503);
            return;
        }
        chain.doFilter(request, response);
    }

    private void deny(HttpServletResponse response, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"sucesso\":false,\"mensagem\":\"" +
                (status == 503 ? "Não foi possível verificar o plano." : "Acesso ao módulo não autorizado.") + "\"}");
    }

    private String path(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
