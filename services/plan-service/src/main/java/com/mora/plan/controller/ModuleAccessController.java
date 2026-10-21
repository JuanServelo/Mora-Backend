package com.mora.plan.controller;

import com.mora.plan.security.AuthContext;
import com.mora.plan.service.AssinaturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plans/access")
@RequiredArgsConstructor
public class ModuleAccessController {
    private final AssinaturaService assinaturas;

    /** Tenant comes exclusively from the verified token, never from a request parameter. */
    @GetMapping("/{module}")
    public ResponseEntity<Void> check(@PathVariable String module) {
        var claims = AuthContext.get();
        if (claims == null) return ResponseEntity.status(401).build();
        if ("ADMIN_GERAL".equals(claims.perfil())) return ResponseEntity.noContent().build();
        if (claims.condominioId() == null || !assinaturas.modulosPermitidos(claims.condominioId()).contains(module)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.noContent().build();
    }
}
