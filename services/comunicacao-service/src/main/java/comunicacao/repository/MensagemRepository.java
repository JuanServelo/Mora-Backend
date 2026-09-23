package comunicacao.repository;

import comunicacao.model.Mensagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MensagemRepository extends JpaRepository<Mensagem, Long> {

    List<Mensagem> findByConversaIdOrderByCriadaEmAsc(Long conversaId);

    /** A última mensagem visível — vira o resumo na lista de conversas. */
    Mensagem findFirstByConversaIdAndRemovidaEmIsNullOrderByCriadaEmDesc(Long conversaId);

    /**
     * Mensagens de outra pessoa chegadas depois da marca d'água.
     *
     * São duas consultas, e não uma com `:desde IS NULL`, por um motivo do
     * driver: o Postgres não consegue inferir o tipo de um parâmetro solto numa
     * comparação com NULL e responde `could not determine data type of
     * parameter`. Daria para resolver com CAST dentro do JPQL, mas duas
     * consultas nomeadas dizem melhor o que cada caso é.
     */
    @Query("""
        SELECT COUNT(m) FROM Mensagem m
         WHERE m.conversaId = :conversaId
           AND m.removidaEm IS NULL
           AND m.autorId <> :usuarioId
           AND m.criadaEm > :desde
        """)
    long contarNaoLidasDesde(@Param("conversaId") Long conversaId,
                             @Param("usuarioId") String usuarioId,
                             @Param("desde") LocalDateTime desde);

    /**
     * Sem marca d'água, tudo de outra pessoa conta como não lido.
     *
     * É o caso da gestão que enxerga a conversa pela regra do condomínio e
     * ainda não respondeu: não há linha em `conversa_participantes`, logo não
     * há leitura registrada — e é correto que tudo apareça como pendente.
     */
    @Query("""
        SELECT COUNT(m) FROM Mensagem m
         WHERE m.conversaId = :conversaId
           AND m.removidaEm IS NULL
           AND m.autorId <> :usuarioId
        """)
    long contarTodasDeOutros(@Param("conversaId") Long conversaId,
                             @Param("usuarioId") String usuarioId);
}
