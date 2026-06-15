package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import portaria.model.Aviso;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AvisoRepository extends JpaRepository<Aviso, UUID> {

    List<Aviso> findByCondominioIdOrderByCriadoEmDesc(String condominioId);

    // Vigentes: publicados e dentro do período de exibição.
    @Query("SELECT a FROM Aviso a WHERE a.condominioId = :condominioId "
            + "AND a.publicado = true AND a.dataInicio <= :hoje AND a.dataFim >= :hoje "
            + "ORDER BY a.dataInicio DESC")
    List<Aviso> findAtivos(@Param("condominioId") String condominioId, @Param("hoje") LocalDate hoje);
}
