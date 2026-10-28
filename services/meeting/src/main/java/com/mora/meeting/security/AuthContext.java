package com.mora.meeting.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class AuthContext {
    public record Actor(Long usuarioId, String perfil, String condominioId, String token) {}
    private static final ThreadLocal<Actor> CURRENT = new ThreadLocal<>();
    private AuthContext() {}
    public static void set(Actor actor) { CURRENT.set(actor); }
    public static void clear() { CURRENT.remove(); }
    public static Actor get() {
        Actor actor = CURRENT.get();
        if (actor == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação obrigatória.");
        return actor;
    }
    public static String tenant() {
        String tenant = get().condominioId();
        if (tenant == null || tenant.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário sem condomínio vinculado.");
        }
        return tenant;
    }
    public static boolean isManager() {
        return "ADMIN_SINDICO".equals(get().perfil()) || "ADMIN_GERAL".equals(get().perfil());
    }
    public static void requireManager() {
        if (!isManager()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas a administração pode gerenciar reuniões e votações.");
        tenant();
    }
    public static void requireSelf(Long id) {
        if (!get().usuarioId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só é permitido responder em seu próprio nome.");
        }
    }
}
