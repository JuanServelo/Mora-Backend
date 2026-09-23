package com.mora.plan.repository;

import com.mora.plan.entity.Subscription;
import com.mora.plan.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByCondominioId(String condominioId);

    Optional<Subscription> findByCondominioIdAndStatus(String condominioId, SubscriptionStatus status);

    boolean existsByCondominioIdAndStatus(String condominioId, SubscriptionStatus status);
}
