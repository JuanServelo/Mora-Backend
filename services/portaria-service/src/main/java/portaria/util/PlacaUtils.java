package portaria.util;

import portaria.exception.OperacaoInvalidaException;

import java.util.regex.Pattern;

/**
 * Normalização e validação de placa, compartilhada entre os serviços que a
 * recebem do cliente (veículos, meus-veículos e pré-liberação).
 *
 * A comparação de placa em todo o sistema pressupõe a forma normalizada:
 * maiúsculas, sem espaço nem hífen. Normalizar em um lugar só evita que
 * "ABC-1234" e "abc1234" virem dois veículos diferentes.
 */
public final class PlacaUtils {

    private static final Pattern PLACA_PATTERN =
            Pattern.compile("^[A-Z]{3}[0-9]{4}$|^[A-Z]{3}[0-9][A-Z][0-9]{2}$");

    private PlacaUtils() {}

    public static String normalizar(String placa) {
        if (placa == null) return null;
        return placa.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    public static boolean valida(String placaNormalizada) {
        return placaNormalizada != null && PLACA_PATTERN.matcher(placaNormalizada).matches();
    }

    /** Normaliza e valida, lançando 409 com mensagem pronta para a tela. */
    public static String normalizarEValidar(String placa) {
        String norm = normalizar(placa);
        if (!valida(norm)) {
            throw new OperacaoInvalidaException(
                    "Placa inválida. Use o formato AAA-9999 ou AAA9A99 (Mercosul).");
        }
        return norm;
    }
}
