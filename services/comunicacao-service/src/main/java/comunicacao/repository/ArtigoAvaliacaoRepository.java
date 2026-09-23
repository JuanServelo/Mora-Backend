package comunicacao.repository;

import comunicacao.model.ArtigoAvaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtigoAvaliacaoRepository extends JpaRepository<ArtigoAvaliacao, UUID> {

    Optional<ArtigoAvaliacao> findByArtigoIdAndUsuarioId(UUID artigoId, String usuarioId);

    List<ArtigoAvaliacao> findByUsuarioIdAndArtigoIdIn(String usuarioId, Collection<UUID> artigoIds);

    long countByArtigoIdAndUtil(UUID artigoId, boolean util);

    /** Cada linha: [artigoId, votos úteis, votos não úteis]. */
    @Query("""
        SELECT a.artigoId,
               SUM(CASE WHEN a.util = true THEN 1 ELSE 0 END),
               SUM(CASE WHEN a.util = false THEN 1 ELSE 0 END)
        FROM ArtigoAvaliacao a
        WHERE a.artigoId IN :artigoIds
        GROUP BY a.artigoId
        """)
    List<Object[]> contarPorArtigo(@Param("artigoIds") Collection<UUID> artigoIds);

    @Modifying
    void deleteByArtigoId(UUID artigoId);
}
