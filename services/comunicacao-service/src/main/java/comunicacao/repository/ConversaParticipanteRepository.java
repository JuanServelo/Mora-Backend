package comunicacao.repository;

import comunicacao.model.ConversaParticipante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversaParticipanteRepository extends JpaRepository<ConversaParticipante, String> {

    List<ConversaParticipante> findByConversaId(Long conversaId);

    Optional<ConversaParticipante> findByConversaIdAndUsuarioId(Long conversaId, String usuarioId);

    boolean existsByConversaIdAndUsuarioId(Long conversaId, String usuarioId);
}
