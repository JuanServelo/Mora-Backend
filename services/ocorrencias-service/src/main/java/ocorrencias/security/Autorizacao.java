package ocorrencias.security;

import ocorrencias.exception.AcessoNegadoException;
import ocorrencias.exception.OperacaoInvalidaException;

/**
 * Quem pode o quê, em um lugar só.
 *
 * As regras ficam aqui, e não espalhadas pelos controllers, porque espalhadas
 * elas divergem: a checagem entra em quatro rotas, esquece a quinta, e a quinta
 * é a que alguém encontra.
 *
 * - **Morador** (e qualquer usuário do condomínio) registra ocorrência e
 *   acompanha as próprias.
 * - **Síndico** atende: responde, muda status, abre e conduz ordem de serviço.
 * - **Admin Geral** acompanha todos os condomínios, mas não fala por nenhum —
 *   a mesma regra do comunicacao-service.
 */
public final class Autorizacao {

    private static final String SINDICO = "ADMIN_SINDICO";
    private static final String ADMIN_GERAL = "ADMIN_GERAL";

    private Autorizacao() {}

    /** Quem está na requisição. Sem token o filtro já respondeu 401. */
    public static JwtClaims atual() {
        JwtClaims claims = AuthContext.get();
        if (claims == null || claims.authUserId() == null) {
            throw new OperacaoInvalidaException("Usuário não autenticado.");
        }
        return claims;
    }

    public static String perfilAtual() {
        JwtClaims claims = AuthContext.get();
        return claims == null ? null : claims.perfil();
    }

    public static boolean ehAdminGeral() {
        return ADMIN_GERAL.equals(perfilAtual());
    }

    public static boolean ehSindico() {
        return SINDICO.equals(perfilAtual());
    }

    /** Síndico ou Admin Geral: enxerga a visão da administração. */
    public static boolean ehGestao() {
        return ehSindico() || ehAdminGeral();
    }

    /** Só o síndico escreve em nome do condomínio. */
    public static void exigirSindico(String operacao) {
        if (!ehSindico()) {
            throw new AcessoNegadoException("Apenas a administração do condomínio pode " + operacao + ".");
        }
    }

    /** Acompanhar é da gestão: síndico ou Admin Geral. */
    public static void exigirVisaoDeGestao(String operacao) {
        if (!ehGestao()) {
            throw new AcessoNegadoException("Apenas a administração pode " + operacao + ".");
        }
    }

    /**
     * O condomínio de quem escreve. Toda escrita precisa de um: sem ele o
     * registro nasceria órfão e não apareceria para ninguém.
     */
    public static String exigirCondominio() {
        String cid = CondominioUtils.condominioIdEfetivo();
        if (cid == null) {
            throw new OperacaoInvalidaException("Seu usuário não está vinculado a um condomínio.");
        }
        return cid;
    }

    /**
     * O recorte de leitura da gestão.
     *
     * O síndico vê o próprio condomínio, sempre — o parâmetro é ignorado para
     * ele. O Admin Geral escolhe pelo parâmetro; sem ele, `null` = todos.
     */
    public static String condominioDeLeitura(String pedido) {
        if (ehAdminGeral()) {
            return pedido == null || pedido.isBlank() ? null : pedido;
        }
        return exigirCondominio();
    }

    /**
     * Se a gestão alcança um registro deste condomínio.
     *
     * Quem não alcança recebe 404, não 403: quem não pode ver não deve
     * descobrir que existe.
     */
    public static boolean gestaoAlcanca(String condominioId) {
        if (ehAdminGeral()) return true;
        return ehSindico() && condominioId != null && condominioId.equals(CondominioUtils.condominioIdEfetivo());
    }
}
