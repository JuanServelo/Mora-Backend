package comunicacao.exception;

/** O usuário está autenticado, mas o perfil dele não pode fazer esta ação (403). */
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String message) { super(message); }
}
