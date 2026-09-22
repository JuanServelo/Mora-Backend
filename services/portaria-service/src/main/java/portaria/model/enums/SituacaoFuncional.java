package portaria.model.enums;

/**
 * Situação funcional (RN-02). Só ATIVO permite registrar entrada — as demais
 * bloqueiam, cada uma com sua mensagem, para o porteiro saber se cabe acionar
 * o síndico ou se o caso é definitivo.
 *
 * Nenhuma delas impede a SAÍDA: quem está dentro sai sempre (RN-04).
 */
public enum SituacaoFuncional {
    ATIVO,
    SUSPENSO,
    AFASTADO,
    DEMITIDO;

    public boolean permiteEntrada() {
        return this == ATIVO;
    }

    /** Motivo pronto para a tela do porteiro. */
    public String motivoBloqueio(String nome) {
        return switch (this) {
            case ATIVO -> null;
            case SUSPENSO -> nome + " está suspenso e não pode entrar.";
            case AFASTADO -> nome + " está afastado e não pode entrar.";
            case DEMITIDO -> nome + " não faz mais parte do quadro de funcionários.";
        };
    }
}
