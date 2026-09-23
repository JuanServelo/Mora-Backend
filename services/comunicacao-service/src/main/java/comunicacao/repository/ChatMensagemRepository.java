package comunicacao.repository;

import comunicacao.model.ChatMensagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMensagemRepository extends JpaRepository<ChatMensagem, String> {

    @Query("""
        SELECT m FROM ChatMensagem m
        WHERE (m.remetenteId = :usuarioA AND m.destinatarioId = :usuarioB)
           OR (m.remetenteId = :usuarioB AND m.destinatarioId = :usuarioA)
        ORDER BY m.enviadoEm ASC
        """)
    List<ChatMensagem> findConversa(@Param("usuarioA") String usuarioA,
                                    @Param("usuarioB") String usuarioB);

    List<ChatMensagem> findByCondominioIdOrderByEnviadoEmDesc(String condominioId);

    List<ChatMensagem> findByDestinatarioIdAndLidaFalse(String destinatarioId);

    long countByDestinatarioIdAndLidaFalse(String destinatarioId);
}
