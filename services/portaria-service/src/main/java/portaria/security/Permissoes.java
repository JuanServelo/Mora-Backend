package portaria.security;

import portaria.exception.AcessoNegadoException;
import portaria.exception.OperacaoInvalidaException;

import java.util.List;

/**
 * Quem pode escrever, e em qual condomínio.
 *
 * Até aqui a leitura era escopada por {@link CondominioUtils} mas a escrita não
 * verificava nada: qualquer autenticado cadastrava bloco, vaga ou funcionário,
 * e em qualquer condomínio, porque o {@code condominioId} vinha no corpo da
 * requisição. Esta classe fecha os dois buracos num lugar só — espalhar a regra
 * pelos doze serviços é o que faria o décimo terceiro nascer sem ela.
 *
 * Os conjuntos são constantes nomeadas, não um sistema de permissões
 * configurável: o projeto não tem onde guardar isso, e inventar um cadastro de
 * permissões para doze serviços seria desproporcional ao problema.
 */
public final class Permissoes {

    /** Opera a estrutura do condomínio: blocos, unidades, vagas, equipe. */
    public static final List<String> PERFIS_ADMIN = List.of("ADMIN_GERAL", "ADMIN_SINDICO");

    /** Opera o dia a dia da portaria, além do que os admins já podem. */
    public static final List<String> PERFIS_OPERACAO =
            List.of("ADMIN_GERAL", "ADMIN_SINDICO", "PORTEIRO");

    private Permissoes() {}

    private static JwtClaims claims() {
        JwtClaims c = AuthContext.get();
        if (c == null || c.perfil() == null) {
            throw new AcessoNegadoException("Autenticação necessária.");
        }
        return c;
    }

    public static boolean ehAdmin() {
        return PERFIS_ADMIN.contains(claims().perfil());
    }

    public static boolean ehOperacao() {
        return PERFIS_OPERACAO.contains(claims().perfil());
    }

    /**
     * Exige perfil administrativo. A mensagem nomeia a ação porque "acesso
     * negado" sozinho não diz à pessoa o que ela tentou fazer.
     */
    public static void exigirAdmin(String acao) {
        if (!PERFIS_ADMIN.contains(claims().perfil())) {
            throw new AcessoNegadoException(
                    "Seu perfil não pode " + acao + ". Esta ação é da administração do condomínio.");
        }
    }

    /** Exige perfil administrativo ou porteiro. */
    public static void exigirOperacao(String acao) {
        if (!PERFIS_OPERACAO.contains(claims().perfil())) {
            throw new AcessoNegadoException(
                    "Seu perfil não pode " + acao + ". Esta ação é da portaria ou da administração.");
        }
    }

    /**
     * Condomínio em que a gravação deve cair.
     *
     * Para quem tem condomínio na claim, o corpo da requisição é ignorado — é o
     * que impede gravar no condomínio alheio trocando um campo do JSON. O
     * ADMIN_GERAL não tem condomínio próprio, então escolhe pelo corpo; sem
     * informar, a gravação é recusada em vez de nascer órfã.
     */
    public static String condominioParaGravar(String doCorpo) {
        String efetivo = CondominioUtils.condominioIdEfetivo();
        if (efetivo != null) return efetivo;
        if (doCorpo != null && !doCorpo.isBlank()) return doCorpo;
        throw new OperacaoInvalidaException(
                "Informe o condomínio para o qual este registro será criado.");
    }

    /**
     * Guarda de alteração: o registro existente precisa ser do condomínio de
     * quem está alterando. Sem isto, escopar a criação não adiantaria — bastaria
     * editar pelo id um registro de outro condomínio.
     */
    public static void exigirMesmoCondominio(String condominioIdDoRegistro) {
        String efetivo = CondominioUtils.condominioIdEfetivo();
        if (efetivo == null) return; // ADMIN_GERAL enxerga todos
        if (condominioIdDoRegistro == null || !efetivo.equals(condominioIdDoRegistro)) {
            throw new AcessoNegadoException("Este registro pertence a outro condomínio.");
        }
    }
}
