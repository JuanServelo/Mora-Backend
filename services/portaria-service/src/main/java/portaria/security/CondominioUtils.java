package portaria.security;

/**
 * Extrai o condominioId efetivo do JWT armazenado no AuthContext.
 *
 * ADMIN_GERAL é identificado pelo condominioId "default" ou null — nesses casos
 * este método retorna null para indicar "sem filtro" (acesso a todos).
 * Para todos os outros perfis, retorna o condominioId da claim, garantindo isolamento.
 */
public final class CondominioUtils {

    private static final String ADMIN_GERAL_CONDOMINIO = "default";

    private CondominioUtils() {}

    public static String condominioIdEfetivo() {
        JwtClaims claims = AuthContext.get();
        if (claims == null) return null;
        String cid = claims.condominioId();
        if (cid == null || ADMIN_GERAL_CONDOMINIO.equals(cid)) return null;
        return cid;
    }
}
