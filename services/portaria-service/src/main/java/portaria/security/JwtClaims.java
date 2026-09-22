package portaria.security;

/**
 * unidadeId = id do apartamento do morador; nulo para perfis sem unidade.
 * nome = usado para registrar autoria de ações sem consultar o auth-api.
 */
public record JwtClaims(String authUserId, String email, String perfil,
                        String condominioId, String unidadeId, String nome) {

    /** Nome quando houver; senão o e-mail, que sempre identifica alguém. */
    public String exibicao() {
        if (nome != null && !nome.isBlank()) return nome;
        return email != null ? email : "sistema";
    }
}
