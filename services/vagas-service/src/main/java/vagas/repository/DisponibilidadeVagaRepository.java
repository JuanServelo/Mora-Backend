package vagas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vagas.model.DisponibilidadeVaga;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DisponibilidadeVagaRepository extends JpaRepository<DisponibilidadeVaga, String> {

    List<DisponibilidadeVaga> findByProprietarioIdAndAtivaTrue(UUID proprietarioId);

    /** Retorna todas as disponibilidades ativas (para listagem pública de vagas disponíveis). */
    List<DisponibilidadeVaga> findByAtivaTrue();

    /**
     * Verifica conflito de datas para a mesma vaga (RF10 CA2).
     * Considera tanto disponibilidades ativas quanto aluguéis aprovados (tratado no service).
     */
    @Query("""
            SELECT d FROM DisponibilidadeVaga d
            WHERE d.vaga.id = :vagaId
              AND d.ativa = true
              AND d.dataInicio <= :dataFim
              AND d.dataFim   >= :dataInicio
            """)
    List<DisponibilidadeVaga> findConflitantes(
            @Param("vagaId") String vagaId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim);
}

