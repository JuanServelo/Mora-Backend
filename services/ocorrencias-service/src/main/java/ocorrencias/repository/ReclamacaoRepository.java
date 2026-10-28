package ocorrencias.repository;

import ocorrencias.model.Reclamacao;
import ocorrencias.model.enums.StatusReclamacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReclamacaoRepository extends JpaRepository<Reclamacao, Long> {

    boolean existsByProtocolo(String protocolo);

    @EntityGraph(attributePaths = "interacoes")
    List<Reclamacao> findByUsuarioIdOrderByCriadoEmDesc(String usuarioId);

    /** `condominioId` nulo = todos (Admin Geral); `status` nulo = qualquer um. */
    @EntityGraph(attributePaths = "interacoes")
    @Query("""
            SELECT r FROM Reclamacao r
             WHERE (:condominioId IS NULL OR r.condominioId = :condominioId)
               AND (:status IS NULL OR r.status = :status)
             ORDER BY r.criadoEm DESC
            """)
    List<Reclamacao> listarDaGestao(@Param("condominioId") String condominioId,
                                    @Param("status") StatusReclamacao status);

    @EntityGraph(attributePaths = "interacoes")
    Optional<Reclamacao> findWithInteracoesById(Long id);
}
