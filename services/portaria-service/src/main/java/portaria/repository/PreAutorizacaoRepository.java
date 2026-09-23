package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import portaria.model.PreAutorizacao;

import java.time.LocalDate;
import java.util.List;

public interface PreAutorizacaoRepository extends JpaRepository<PreAutorizacao, String> {

    List<PreAutorizacao> findByMoradorId(String moradorId);

    List<PreAutorizacao> findByUnidadeId(String unidadeId);

    // "Ativa hoje" = aguardando e dentro da validade. Vencida não aparece:
    // statusEfetivo() a trataria como EXPIRADA de qualquer forma.
    @Query("""
        SELECT p FROM PreAutorizacao p
        WHERE p.condominioId = :condominioId
          AND p.status = portaria.model.enums.StatusPreAutorizacao.AGUARDANDO
          AND p.validadeInicio <= :hoje
          AND p.validadeFim   >= :hoje
        """)
    List<PreAutorizacao> findAtivasHoje(@Param("condominioId") String condominioId,
                                        @Param("hoje") LocalDate hoje);

    @Query("""
        SELECT p FROM PreAutorizacao p
        WHERE p.status = portaria.model.enums.StatusPreAutorizacao.AGUARDANDO
          AND p.validadeInicio <= :hoje
          AND p.validadeFim   >= :hoje
        """)
    List<PreAutorizacao> findTodasAtivasHoje(@Param("hoje") LocalDate hoje);

    @Query("""
        SELECT p FROM PreAutorizacao p
        WHERE p.condominioId = :condominioId
          AND p.status = portaria.model.enums.StatusPreAutorizacao.AGUARDANDO
          AND p.validadeInicio <= :hoje
          AND p.validadeFim   >= :hoje
          AND (LOWER(p.nomeVisitante) LIKE LOWER(CONCAT('%', :termo, '%'))
               OR p.cpfVisitante = :termo)
        """)
    List<PreAutorizacao> buscarPorNomeOuCpf(@Param("condominioId") String condominioId,
                                             @Param("hoje") LocalDate hoje,
                                             @Param("termo") String termo);

    /** Atendimento: autorização ativa ao identificar o visitante pelo CPF (RN-08). */
    @Query("""
        SELECT p FROM PreAutorizacao p
        WHERE (:condominioId IS NULL OR p.condominioId = :condominioId)
          AND p.status = portaria.model.enums.StatusPreAutorizacao.AGUARDANDO
          AND p.validadeInicio <= :hoje
          AND p.validadeFim   >= :hoje
          AND p.cpfVisitante = :cpf
        """)
    List<PreAutorizacao> buscarPorCpf(@Param("condominioId") String condominioId,
                                      @Param("hoje") LocalDate hoje,
                                      @Param("cpf") String cpf);

    /** Usada pelo atendimento para destacar a autorização ao bipar a placa (RN-08). */
    @Query("""
        SELECT p FROM PreAutorizacao p
        WHERE (:condominioId IS NULL OR p.condominioId = :condominioId)
          AND p.status = portaria.model.enums.StatusPreAutorizacao.AGUARDANDO
          AND p.validadeInicio <= :hoje
          AND p.validadeFim   >= :hoje
          AND UPPER(p.placaVeiculo) = UPPER(:placa)
        """)
    List<PreAutorizacao> buscarPorPlaca(@Param("condominioId") String condominioId,
                                        @Param("hoje") LocalDate hoje,
                                        @Param("placa") String placa);
}
