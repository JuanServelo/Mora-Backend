package comunicacao.repository;

import comunicacao.model.Conversa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversaRepository extends JpaRepository<Conversa, Long> {

    Optional<Conversa> findByIdAndCondominioId(Long id, String condominioId);

    /**
     * As conversas que este usuário enxerga.
     *
     * Duas portas, e a segunda é a que sustenta o formato `ADMINISTRACAO`: ou a
     * pessoa é participante, ou é gestão do condomínio e a conversa é com a
     * administração. A gestão vê a conversa **antes** de responder — é assim
     * que a pendência chega até ela; vira participante só ao responder.
     */
    @Query("""
        SELECT c FROM Conversa c
         WHERE c.condominioId = :condominioId
           AND (:incluirEncerradas = true OR c.encerradaEm IS NULL)
           AND (
                 EXISTS (SELECT 1 FROM ConversaParticipante p
                          WHERE p.conversaId = c.id AND p.usuarioId = :usuarioId)
                 OR (:ehGestao = true AND c.tipo = comunicacao.model.enums.TipoConversa.ADMINISTRACAO)
               )
         ORDER BY COALESCE(c.ultimaMensagemEm, c.criadaEm) DESC
        """)
    List<Conversa> listarVisiveis(@Param("usuarioId") String usuarioId,
                                  @Param("condominioId") String condominioId,
                                  @Param("ehGestao") boolean ehGestao,
                                  @Param("incluirEncerradas") boolean incluirEncerradas);
}
