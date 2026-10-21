package comunicacao.repository;

import comunicacao.model.AvisoLeitura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AvisoLeituraRepository extends JpaRepository<AvisoLeitura, String> {

    Optional<AvisoLeitura> findByAvisoIdAndUsuarioId(UUID avisoId, String usuarioId);

    List<AvisoLeitura> findByUsuarioId(String usuarioId);

    long countByAvisoId(UUID avisoId);

    @Query("SELECT al.avisoId FROM AvisoLeitura al WHERE al.usuarioId = :usuarioId")
    List<UUID> findAvisosLidosByUsuario(@Param("usuarioId") String usuarioId);

    /** Leituras de um aviso — quem confirmou e quando. */
    List<AvisoLeitura> findByAvisoId(UUID avisoId);

    /**
     * Confirmações por aviso, numa consulta só.
     *
     * O panorama mostra todos os avisos vigentes de uma vez; contar um por um
     * seria uma ida ao banco por aviso.
     */
    @Query("""
        SELECT al.avisoId, COUNT(al) FROM AvisoLeitura al
         WHERE al.avisoId IN :avisos
         GROUP BY al.avisoId
        """)
    List<Object[]> contarPorAviso(@Param("avisos") List<UUID> avisos);
}
