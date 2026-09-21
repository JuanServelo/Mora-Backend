package portaria.util;

import portaria.exception.OperacaoInvalidaException;

/**
 * Normalização e validação de CPF, compartilhada entre o atendimento da
 * portaria e a pré-liberação feita pelo morador.
 *
 * As duas telas produzem o mesmo cadastro de visitante, então precisam aceitar
 * e recusar exatamente os mesmos documentos — duas cópias da regra divergiriam
 * na primeira correção.
 */
public final class CpfUtils {

    private CpfUtils() {}

    /** Só dígitos; o formato original fica preservado em outro campo. */
    public static String normalizar(String cpf) {
        if (cpf == null) return null;
        return cpf.replaceAll("\\D", "");
    }

    public static boolean valido(String cpfNormalizado) {
        if (cpfNormalizado == null || !cpfNormalizado.matches("\\d{11}")) return false;
        if (cpfNormalizado.chars().distinct().count() == 1) return false;

        int sum = 0;
        for (int i = 0; i < 9; i++) sum += (cpfNormalizado.charAt(i) - '0') * (10 - i);
        int rem = 11 - (sum % 11);
        int dig1 = rem >= 10 ? 0 : rem;
        if (dig1 != cpfNormalizado.charAt(9) - '0') return false;

        sum = 0;
        for (int i = 0; i < 10; i++) sum += (cpfNormalizado.charAt(i) - '0') * (11 - i);
        rem = 11 - (sum % 11);
        int dig2 = rem >= 10 ? 0 : rem;
        return dig2 == cpfNormalizado.charAt(10) - '0';
    }

    /** Normaliza e valida, com a mensagem já pronta para a tela. */
    public static String normalizarEValidar(String cpf) {
        String norm = normalizar(cpf);
        if (!valido(norm)) {
            throw new OperacaoInvalidaException("CPF inválido. Verifique os dígitos informados.");
        }
        return norm;
    }
}
