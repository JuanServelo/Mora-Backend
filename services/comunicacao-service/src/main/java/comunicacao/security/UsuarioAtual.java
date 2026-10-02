package comunicacao.security;

import comunicacao.exception.AcessoNegadoException;
import comunicacao.exception.OperacaoInvalidaException;

import java.util.Set;

/**
 * Quem está fazendo a requisição, lido do token.
 *
 * O id do usuário é tratado como texto, e não como UUID: no auth-api ele é um
 * inteiro ("13"), e `UUID.fromString` quebraria para qualquer usuário real.
 */
public final class UsuarioAtual {

    private static final Set<String> PERFIS_GESTAO = Set.of("ADMIN_SINDICO", "ADMIN_GERAL");

    private UsuarioAtual() {}

    public static JwtClaims claims() {
        JwtClaims claims = AuthContext.get();
        if (claims == null || claims.authUserId() == null || "null".equals(claims.authUserId())) {
            throw new OperacaoInvalidaException("Usuário não autenticado");
        }
        return claims;
    }

    public static String id() {
        return claims().authUserId();
    }

    /** Nome para exibição; cai para o e-mail em tokens antigos, emitidos sem o nome. */
    public static String nome() {
        JwtClaims c = claims();
        return c.nome() != null && !c.nome().isBlank() ? c.nome() : c.email();
    }

    public static boolean ehGestao() {
        return PERFIS_GESTAO.contains(claims().perfil());
    }

    public static void exigirGestao() {
        if (!ehGestao()) {
            throw new AcessoNegadoException("Apenas a administração do condomínio pode fazer isso");
        }
    }
}
