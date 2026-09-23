package comunicacao.dto;

/** Identidade mínima de um usuário, vinda do auth-api. */
public record UsuarioResumo(
        String id,
        String nome,
        String perfil,
        String fotoUrl,
        String bloco,
        String apartamento
) {
    /** Placeholder para quando o auth-api não conhece mais o id (usuário removido). */
    public static UsuarioResumo desconhecido(String id) {
        return new UsuarioResumo(id, "Usuário " + id, null, null, null, null);
    }

    public String unidade() {
        if (bloco == null && apartamento == null) return null;
        if (bloco == null) return apartamento;
        if (apartamento == null) return bloco;
        return bloco + " " + apartamento;
    }
}
