package com.mora.plan.repository;

import com.mora.plan.entity.Assinatura;
import com.mora.plan.enums.StatusAssinatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssinaturaRepository extends JpaRepository<Assinatura, Long> {

    Optional<Assinatura> findByCondominioIdAndStatus(String condominioId, StatusAssinatura status);

    List<Assinatura> findByCondominioIdOrderByCreatedAtDesc(String condominioId);

    List<Assinatura> findByStatus(StatusAssinatura status);

    long countByPlanIdAndStatus(Long planId, StatusAssinatura status);
}
