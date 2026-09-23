package portaria.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import portaria.model.Reserva;
import portaria.model.enums.StatusReserva;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, String> {

    List<Reserva> findBySolicitanteId(String solicitanteId);

    List<Reserva> findByCondominioId(String condominioId);

    List<Reserva> findByCondominioIdAndStatus(String condominioId, StatusReserva status);

    List<Reserva> findByAreaComum_Id(String areaComumId);

    List<Reserva> findByUnidadeId(String unidadeId);

    /**
     * Agenda: tudo que toca a janela exibida, inclusive reservas passadas.
     *
     * Canceladas e recusadas ficam de fora: elas não ocupam o horário, e
     * mostrá-las faria o porteiro ler como indisponível um espaço livre.
     * É o mesmo recorte de findConflitantes — agenda e conflito concordam.
     */
    @Query("""
        SELECT r FROM Reserva r
        WHERE r.areaComum.id = :areaComumId
          AND r.status IN (
                portaria.model.enums.StatusReserva.PENDENTE,
                portaria.model.enums.StatusReserva.APROVADA,
                portaria.model.enums.StatusReserva.CONCLUIDA)
          AND r.inicio < :fim
          AND r.fim    > :inicio
        ORDER BY r.inicio ASC
        """)
    List<Reserva> findNoPeriodo(@Param("areaComumId") String areaComumId,
                                @Param("inicio") LocalDateTime inicio,
                                @Param("fim") LocalDateTime fim);

    /**
     * Sobreposição real de intervalos: inicio_novo < fim_existente E
     * fim_novo > inicio_existente. Canceladas e recusadas não ocupam.
     *
     * PESSIMISTIC_WRITE serializa as verificações concorrentes do mesmo
     * espaço — sem isso, duas requisições simultâneas passariam as duas pela
     * checagem antes de qualquer uma gravar.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM Reserva r
        WHERE r.areaComum.id = :areaComumId
          AND r.status IN (
                portaria.model.enums.StatusReserva.PENDENTE,
                portaria.model.enums.StatusReserva.APROVADA,
                portaria.model.enums.StatusReserva.CONCLUIDA)
          AND (:ignorarId IS NULL OR r.id <> :ignorarId)
          AND r.inicio < :fim
          AND r.fim    > :inicio
        """)
    List<Reserva> findConflitantes(@Param("areaComumId") String areaComumId,
                                   @Param("inicio") LocalDateTime inicio,
                                   @Param("fim") LocalDateTime fim,
                                   @Param("ignorarId") String ignorarId);

    /**
     * Pendentes do condomínio, para a fila do síndico (RN-08).
     * Ordenadas pela proximidade do início: são as que expiram antes.
     */
    @Query("""
        SELECT r FROM Reserva r
        WHERE r.condominioId = :condominioId
          AND r.status = portaria.model.enums.StatusReserva.PENDENTE
        ORDER BY r.inicio ASC
        """)
    List<Reserva> findPendentes(@Param("condominioId") String condominioId);

    /**
     * Pendentes cujo prazo de decisão passou. Varridas antes de qualquer
     * checagem de conflito: uma pendente esquecida não pode bloquear o espaço
     * para sempre (RN-05).
     *
     * Duas pontas, e a primeira que vencer encerra:
     *  - criadoEm + PRAZO: o síndico teve o tempo de resposta e não decidiu;
     *  - inicio: a hora reservada chegou sem decisão, então não há mais o que
     *    aprovar — uma reserva não pode ser confirmada depois de começar.
     *
     * Amarrar o prazo à proximidade do início, em vez da criação, apagaria
     * toda solicitação de última hora antes de o síndico abrir a fila.
     */
    @Query("""
        SELECT r FROM Reserva r
        WHERE r.status = portaria.model.enums.StatusReserva.PENDENTE
          AND (r.criadoEm <= :limiteCriacao OR r.inicio <= :agora)
        """)
    List<Reserva> findPendentesVencidas(@Param("limiteCriacao") LocalDateTime limiteCriacao,
                                        @Param("agora") LocalDateTime agora);

    /** Reservas futuras que ainda ocupam horário — guarda da RN-09. */
    @Query("""
        SELECT r FROM Reserva r
        WHERE r.areaComum.id = :areaComumId
          AND r.status IN (
                portaria.model.enums.StatusReserva.PENDENTE,
                portaria.model.enums.StatusReserva.APROVADA,
                portaria.model.enums.StatusReserva.CONCLUIDA)
          AND r.fim > :agora
        ORDER BY r.inicio ASC
        """)
    List<Reserva> findFuturas(@Param("areaComumId") String areaComumId,
                              @Param("agora") LocalDateTime agora);
}
