package com.mora.plan.repository;

import com.mora.plan.entity.Assinatura;
import com.mora.plan.enums.StatusAssinatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssinaturaRepository extends JpaRepository<Assinatura, Long> {

    /**
     * Painel de receita pronto, em JSON, calculado pela função
     * {@code receita_plataforma} (migração V3). Volta como texto porque o
     * Hibernate não mapeia jsonb para String sozinho.
     *
     * O CAST do plano não é enfeite: com o parâmetro nulo, o Postgres não
     * consegue deduzir o tipo e recusa a consulta.
     */
    @Query(value = "SELECT CAST(receita_plataforma(:meses, CAST(:plano AS varchar)) AS text)", nativeQuery = true)
    String receitaPlataforma(@Param("meses") Integer meses, @Param("plano") String plano);

    Optional<Assinatura> findByCondominioIdAndStatus(String condominioId, StatusAssinatura status);

    List<Assinatura> findByCondominioIdOrderByCreatedAtDesc(String condominioId);

    List<Assinatura> findByStatus(StatusAssinatura status);

    long countByPlanIdAndStatus(Long planId, StatusAssinatura status);
}
