package comunicacao.exception;

/**
 * O usuário está identificado, mas o perfil dele não alcança esta operação.
 *
 * É diferente de não encontrar: **404 é a resposta certa quando o recurso
 * existe e pertence a outro condomínio**, porque dizer "existe, mas não é seu"
 * já entrega que existe. Aqui a operação é que não é dele — o recurso não entra
 * na conversa.
 */
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String message) {
        super(message);
    }
}
