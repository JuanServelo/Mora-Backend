package comunicacao.repository;

import comunicacao.model.ArtigoConhecimento;
import comunicacao.model.enums.CategoriaArtigo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ArtigoConhecimentoRepository extends JpaRepository<ArtigoConhecimento, UUID> {

    List<ArtigoConhecimento> findByCondominioId(String condominioId);

    List<ArtigoConhecimento> findByPublicadoTrue();

    List<ArtigoConhecimento> findByCondominioIdAndPublicadoTrue(String condominioId);

    List<ArtigoConhecimento> findByCategoriaAndPublicadoTrue(CategoriaArtigo categoria);

    List<ArtigoConhecimento> findByCondominioIdAndCategoriaAndPublicadoTrue(
            String condominioId, CategoriaArtigo categoria);

    /**
     * Busca por título. O condominioId nulo é o Admin Geral, que enxerga a
     * plataforma inteira — por isso a condição o aceita em vez de filtrar.
     */
    @Query("""
        SELECT a FROM ArtigoConhecimento a
        WHERE LOWER(a.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))
          AND (:condominioId IS NULL OR a.condominioId = :condominioId)
          AND (:apenasPublicados = false OR a.publicado = true)
        ORDER BY a.titulo ASC
        """)
    List<ArtigoConhecimento> buscarPorTitulo(@Param("titulo") String titulo,
                                             @Param("condominioId") String condominioId,
                                             @Param("apenasPublicados") boolean apenasPublicados);
}
