package comunicacao.repository;

import comunicacao.model.AvisoLeitura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AvisoLeituraRepository extends JpaRepository<AvisoLeitura, String> {

    Optional<AvisoLeitura> findByAvisoIdAndUsuarioId(UUID avisoId, String usuarioId);

    List<AvisoLeitura> findByAvisoIdOrderByLidoEmDesc(UUID avisoId);

    long countByAvisoId(UUID avisoId);

    @Query("SELECT al.avisoId FROM AvisoLeitura al WHERE al.usuarioId = :usuarioId")
    List<UUID> findAvisosLidosByUsuario(@Param("usuarioId") String usuarioId);

    /**
     * Contagem de leituras de vários avisos em uma consulta só — a lista do
     * síndico mostra o indicador em cada linha e não pode fazer N consultas.
     */
    @Query("""
        SELECT al.avisoId, COUNT(al)
        FROM AvisoLeitura al
        WHERE al.avisoId IN :avisoIds
        GROUP BY al.avisoId
        """)
    List<Object[]> contarPorAviso(@Param("avisoIds") Collection<UUID> avisoIds);
}
