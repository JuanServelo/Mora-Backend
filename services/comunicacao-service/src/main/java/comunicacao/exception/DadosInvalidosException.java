package comunicacao.exception;

/**
 * O pedido chegou malformado: falta campo, o valor não serve, o id não tem o
 * formato esperado.
 *
 * Separada de `OperacaoInvalidaException`, que é 409: lá o pedido está correto
 * e o estado é que não permite — responder numa conversa encerrada, por
 * exemplo. Aqui o problema é o próprio pedido, e quem chamou precisa corrigi-lo
 * antes de tentar de novo.
 */
public class DadosInvalidosException extends RuntimeException {
    public DadosInvalidosException(String message) {
        super(message);
    }
}
