package portaria.model.enums;

/**
 * Estados de uma pré-liberação (RN-08).
 *
 * EXPIRADA nunca é gravada: é derivada na leitura a partir da validade.
 * Guardá-la exigiria um job periódico só para virar o estado, e o resultado
 * seria o mesmo que comparar a data no momento da consulta.
 */
public enum StatusPreAutorizacao {
    AGUARDANDO,
    UTILIZADA,
    EXPIRADA,
    CANCELADA
}
