package comunicacao.repository;

import comunicacao.model.PreferenciaNotificacao;
import comunicacao.model.enums.TipoNotificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreferenciaNotificacaoRepository extends JpaRepository<PreferenciaNotificacao, String> {

    List<PreferenciaNotificacao> findByUsuarioId(String usuarioId);

    boolean existsByUsuarioIdAndTipo(String usuarioId, TipoNotificacao tipo);

    void deleteByUsuarioId(String usuarioId);
}
