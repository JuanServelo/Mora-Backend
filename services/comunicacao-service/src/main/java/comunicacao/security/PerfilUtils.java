package comunicacao.security;

import comunicacao.exception.OperacaoInvalidaException;

import java.util.List;

/** Perfis do auth-api — espelho de services/auth-api/constants/perfis.js. */
public final class PerfilUtils {

    public static final String ADMIN_GERAL   = "ADMIN_GERAL";
    public static final String ADMIN_SINDICO = "ADMIN_SINDICO";
    public static final String PORTEIRO      = "PORTEIRO";

    /** Publicam avisos e artigos, e respondem no chat pela administração. */
    private static final List<String> ADMINISTRACAO = List.of(ADMIN_GERAL, ADMIN_SINDICO, PORTEIRO);

    /** Criam e editam conteúdo oficial. O porteiro lê, não publica. */
    private static final List<String> GESTORES = List.of(ADMIN_GERAL, ADMIN_SINDICO);

    private PerfilUtils() {}

    public static String perfilAtual() {
        JwtClaims claims = AuthContext.get();
        return claims == null ? null : claims.perfil();
    }

    public static boolean isAdministracao(String perfil) {
        return ADMINISTRACAO.contains(perfil);
    }

    public static boolean isGestor(String perfil) {
        return GESTORES.contains(perfil);
    }

    /** Barra quem não gere o condomínio de publicar em nome dele. */
    public static void exigirGestor() {
        if (!isGestor(perfilAtual())) {
            throw new AcessoNegadoException("Apenas a administração do condomínio pode realizar esta operação.");
        }
    }

    public static String usuarioAtual() {
        JwtClaims claims = AuthContext.get();
        if (claims == null || claims.authUserId() == null || "null".equals(claims.authUserId())) {
            throw new OperacaoInvalidaException("Usuário não autenticado");
        }
        return claims.authUserId();
    }
}
