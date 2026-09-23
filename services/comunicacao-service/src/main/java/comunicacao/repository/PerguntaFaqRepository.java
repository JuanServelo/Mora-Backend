package comunicacao.repository;

import comunicacao.model.PerguntaFaq;
import comunicacao.model.enums.StatusPergunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PerguntaFaqRepository extends JpaRepository<PerguntaFaq, UUID> {

    List<PerguntaFaq> findByAutorIdOrderByCriadoEmDesc(String autorId);

    List<PerguntaFaq> findByCondominioIdOrderByCriadoEmDesc(String condominioId);

    List<PerguntaFaq> findByCondominioIdAndStatusOrderByCriadoEmDesc(String condominioId, StatusPergunta status);

    List<PerguntaFaq> findAllByOrderByCriadoEmDesc();

    List<PerguntaFaq> findByStatusOrderByCriadoEmDesc(StatusPergunta status);

    /** O artigo foi excluído: a pergunta continua respondida, só perde o link. */
    @Modifying
    @Query("UPDATE PerguntaFaq p SET p.artigoId = null WHERE p.artigoId = :artigoId")
    void desvincularArtigo(@Param("artigoId") UUID artigoId);
}
