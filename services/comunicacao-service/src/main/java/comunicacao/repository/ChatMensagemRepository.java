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

    List<ChatMensagem> findByDestinatarioIdAndLidaFalseOrderByEnviadoEmDesc(String destinatarioId);

    long countByDestinatarioIdAndLidaFalse(String destinatarioId);

    /**
     * Todas as mensagens em que o usuário participa, da mais recente para a
     * mais antiga. O agrupamento por interlocutor é feito em memória: o volume
     * é o histórico de um usuário, não o do condomínio inteiro, e assim a
     * consulta continua portável entre bancos.
     */
    @Query("""
        SELECT m FROM ChatMensagem m
        WHERE m.remetenteId = :usuarioId OR m.destinatarioId = :usuarioId
        ORDER BY m.enviadoEm DESC
        """)
    List<ChatMensagem> findEnvolvendo(@Param("usuarioId") String usuarioId);
}
