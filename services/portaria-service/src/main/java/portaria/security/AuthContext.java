package portaria.security;

public class AuthContext {

    private static final ThreadLocal<JwtClaims> holder = new ThreadLocal<>();
    // Token cru: repassado ao auth-api para que ele resolva o próprio chamador,
    // em vez de confiarmos num id de usuário vindo no corpo da requisição.
    private static final ThreadLocal<String> tokenHolder = new ThreadLocal<>();

    public static void set(JwtClaims claims) { holder.set(claims); }
    public static JwtClaims get() { return holder.get(); }

    public static void setToken(String token) { tokenHolder.set(token); }
    public static String getToken() { return tokenHolder.get(); }

    public static void clear() {
        holder.remove();
        tokenHolder.remove();
    }

    private AuthContext() {}
}
