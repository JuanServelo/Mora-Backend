package comunicacao.security;

import comunicacao.exception.AcessoNegadoException;

import java.util.Set;

/**
 * Quem pode o quê, em um lugar só.
 *
 * Até aqui o serviço não tinha checagem de perfil nenhuma: o `AuthFilter`
 * confirmava que havia um token válido e nada mais. Na prática, **qualquer
 * usuário autenticado criava, editava, publicava e excluía comunicado** — um
 * morador inclusive, e o aviso saía assinado com o e-mail dele.
 *
 * As regras ficam aqui, e não espalhadas pelos controllers, porque espalhadas
 * elas divergem: a checagem entra em quatro rotas, esquece a quinta, e a quinta
 * é a que alguém encontra.
 */
public final class Autorizacao {

    /** Publica em nome do condomínio. */
    private static final String SINDICO = "ADMIN_SINDICO";

    /** Opera a plataforma. Acompanha todos os condomínios, não fala por nenhum. */
    private static final String ADMIN_GERAL = "ADMIN_GERAL";

    /**
     * Cadastrados para serem registrados na portaria, não para usar o sistema.
     *
     * `CONVIDADO` é visita e `TERCEIRO` é funcionário terceirizado. Nenhum dos
     * dois tem por que ler o comunicado interno do condomínio.
     */
    private static final Set<String> SEM_ACESSO = Set.of("CONVIDADO", "TERCEIRO");

    private Autorizacao() {}

    /** O perfil de quem está na requisição, ou `null` se não houver token. */
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

    /**
     * Exige quem possa **escrever em nome do condomínio** — só o síndico.
     *
     * O Admin Geral fica de fora de propósito, e não por esquecimento. Ele
     * opera a plataforma: acompanha qualquer condomínio, mas não publica
     * comunicado em nome de nenhum. Há também um motivo mecânico —
     * `CondominioUtils.condominioIdEfetivo()` devolve `null` para ele, então um
     * aviso criado pelo Admin Geral nasceria sem condomínio e não apareceria
     * para ninguém.
     */
    public static void exigirGestaoDoCondominio(String operacao) {
        if (!ehSindico()) {
            throw new AcessoNegadoException(
                    "Apenas a administração do condomínio pode " + operacao + ".");
        }
    }

    /**
     * Se quem está pedindo fala em nome da administração.
     *
     * É a chave da visibilidade das conversas: a gestão enxerga toda conversa
     * do tipo `ADMINISTRACAO` do seu condomínio **sem ser participante** — é
     * assim que a pendência de um morador chega até ela. Vira participante ao
     * responder, não ao abrir.
     */
    public static boolean ehGestao() {
        return ehSindico() || ehAdminGeral();
    }

    /**
     * Exige quem possa **enxergar a visão da administração**: síndico ou Admin
     * Geral.
     *
     * É outra checagem, e não a mesma com um nome diferente. Ver o rascunho de
     * um comunicado e o relatório de leitura é acompanhamento, e o Admin Geral
     * acompanha — o que ele não faz é escrever em nome do condomínio. Usar
     * `exigirGestaoDoCondominio` aqui o barraria da própria função dele.
     */
    public static void exigirVisaoDeGestao(String operacao) {
        if (!ehSindico() && !ehAdminGeral()) {
            throw new AcessoNegadoException(
                    "Apenas a administração pode " + operacao + ".");
        }
    }

    /**
     * Exige alguém que use o sistema.
     *
     * Barra visitante e terceirizado, que existem no cadastro para serem
     * registrados na portaria — não para ler o que a administração comunica aos
     * moradores.
     */
    public static void exigirAcessoAoSistema() {
        String perfil = perfilAtual();
        if (perfil == null || SEM_ACESSO.contains(perfil)) {
            throw new AcessoNegadoException("Seu perfil não tem acesso a esta área.");
        }
    }
}
