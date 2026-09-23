package portaria.model.enums;

/**
 * Quem responde pela reserva (RN-04).
 *
 * MORADOR exige unidade e morador ativo dela; EVENTO_CONDOMINIO exige
 * funcionário responsável e não tem unidade.
 */
public enum TipoReserva {
    MORADOR,
    EVENTO_CONDOMINIO
}
