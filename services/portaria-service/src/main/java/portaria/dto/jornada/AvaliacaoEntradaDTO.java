package portaria.dto.jornada;

/**
 * Resposta da checagem de entrada de funcionário (RN-04).
 *
 * Devolve o motivo, não só um booleano: o porteiro precisa saber se cabe
 * acionar o síndico ou se o caso é definitivo.
 */
public record AvaliacaoEntradaDTO(
        boolean permitido,
        String motivo,
        /** Turno previsto para hoje, gravado como snapshot no acesso. */
        String turnoPrevisto,
        /** Verdadeiro quando entrou por liberação excepcional vigente. */
        boolean porLiberacaoExcepcional,
        String liberacaoMotivo,
        String liberacaoAutorizadaPor
) {
    public static AvaliacaoEntradaDTO permitido(String turnoPrevisto) {
        return new AvaliacaoEntradaDTO(true, null, turnoPrevisto, false, null, null);
    }

    public static AvaliacaoEntradaDTO porLiberacao(String turnoPrevisto, String motivo, String autor) {
        return new AvaliacaoEntradaDTO(true, null, turnoPrevisto, true, motivo, autor);
    }

    public static AvaliacaoEntradaDTO bloqueado(String motivo, String turnoPrevisto) {
        return new AvaliacaoEntradaDTO(false, motivo, turnoPrevisto, false, null, null);
    }
}
