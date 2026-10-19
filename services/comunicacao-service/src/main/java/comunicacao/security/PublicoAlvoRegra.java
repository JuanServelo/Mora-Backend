package comunicacao.security;

import comunicacao.model.enums.PublicoAlvo;

import java.util.Map;
import java.util.Set;

/**
 * Quem é destinatário de um aviso, pelo público dele.
 *
 * É a mesma regra para duas perguntas — quem **recebe** o aviso e quem **entra
 * no denominador** do relatório de leitura. Se cada uma tivesse a sua, o
 * relatório cobraria leitura de quem nunca recebeu o comunicado.
 *
 * A administração não é destinatária de `TODOS`: ela é quem publica. Só o
 * público `SINDICO` a alcança.
 */
public final class PublicoAlvoRegra {

    private static final Set<String> MORADORES = Set.of("MORADOR", "DONO_ALUGUEL");
    private static final Set<String> FUNCIONARIOS = Set.of("PORTEIRO");

    private static final Map<PublicoAlvo, Set<String>> DESTINATARIOS = Map.of(
            PublicoAlvo.TODOS, Set.of("MORADOR", "DONO_ALUGUEL", "PORTEIRO"),
            PublicoAlvo.MORADORES, MORADORES,
            PublicoAlvo.FUNCIONARIOS, FUNCIONARIOS,
            PublicoAlvo.SINDICO, Set.of("ADMIN_SINDICO")
    );

    private PublicoAlvoRegra() {}

    /** Se um usuário com este perfil é destinatário de um aviso com este público. */
    public static boolean ehDestinatario(PublicoAlvo publico, String perfil) {
        if (perfil == null) return false;
        // Aviso antigo sem público gravado vale como TODOS, que é o padrão da entidade.
        Set<String> perfis = DESTINATARIOS.get(publico == null ? PublicoAlvo.TODOS : publico);
        return perfis != null && perfis.contains(perfil);
    }
}
