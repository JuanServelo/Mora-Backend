package portaria.model.enums;

/**
 * Como a área comum restringe horários de reserva (RN-09).
 *
 * SEM_RESTRICAO é o padrão de migração: impor uma janela a espaços já
 * cadastrados poderia invalidar reservas futuras existentes.
 */
public enum ModoFuncionamento {
    SEM_RESTRICAO,
    HORARIOS_DEFINIDOS
}
