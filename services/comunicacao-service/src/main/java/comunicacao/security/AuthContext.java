package comunicacao.security;

/**
 * Identidade do requisitante durante a requisição.
 *
 * Guarda também o token cru: o serviço não tem base de usuários própria e
 * consulta o auth-api em nome de quem chamou, repassando o mesmo Bearer.
 */
public class AuthContext {

    private static final ThreadLocal<JwtClaims> holder = new ThreadLocal<>();
    private static final ThreadLocal<String> tokenHolder = new ThreadLocal<>();

    public static void set(JwtClaims claims) { holder.set(claims); }
    public static JwtClaims get() { return holder.get(); }

    public static void setToken(String token) { tokenHolder.set(token); }
    public static String token() { return tokenHolder.get(); }

    public static void clear() {
        holder.remove();
        tokenHolder.remove();
    }

    private AuthContext() {}
}
