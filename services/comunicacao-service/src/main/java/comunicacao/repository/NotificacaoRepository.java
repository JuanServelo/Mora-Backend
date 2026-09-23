package comunicacao.repository;

import comunicacao.model.Notificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, String> {

    List<Notificacao> findByDestinatarioIdOrderByCriadoEmDesc(String destinatarioId);

    Page<Notificacao> findByDestinatarioIdOrderByCriadoEmDesc(String destinatarioId, Pageable pageable);

    List<Notificacao> findByDestinatarioIdAndLidaFalseOrderByCriadoEmDesc(String destinatarioId);

    long countByDestinatarioIdAndLidaFalse(String destinatarioId);
}
