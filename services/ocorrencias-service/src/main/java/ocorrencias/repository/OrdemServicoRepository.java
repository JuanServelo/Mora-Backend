package ocorrencias.repository;

import ocorrencias.model.OrdemServico;
import ocorrencias.model.enums.StatusOrdemServico;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {

    boolean existsByNumero(String numero);

    @EntityGraph(attributePaths = {"fornecedor", "reclamacao"})
    @Query("""
            SELECT o FROM OrdemServico o
             WHERE (:condominioId IS NULL OR o.condominioId = :condominioId)
               AND (:status IS NULL OR o.status = :status)
             ORDER BY o.criadoEm DESC
            """)
    List<OrdemServico> listar(@Param("condominioId") String condominioId,
                              @Param("status") StatusOrdemServico status);

    @EntityGraph(attributePaths = {"fornecedor", "reclamacao", "andamentos"})
    Optional<OrdemServico> findDetalhadaById(Long id);

    /** As OS ligadas a cada ocorrência, para a ocorrência mostrar o vínculo. */
    List<OrdemServico> findByReclamacaoIdInOrderByCriadoEmAsc(Collection<Long> reclamacaoIds);

    /** Prazo vencido sem conclusão: as que o job marca como atrasadas. */
    @EntityGraph(attributePaths = {"reclamacao"})
    List<OrdemServico> findByStatusInAndPrazoBefore(Collection<StatusOrdemServico> status, LocalDate hoje);
}
