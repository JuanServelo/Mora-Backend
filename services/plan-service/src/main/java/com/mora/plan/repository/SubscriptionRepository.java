package com.mora.plan.repository;

import com.mora.plan.entity.Subscription;
import com.mora.plan.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByCondominioId(String condominioId);
    Optional<Subscription> findByCondominioIdAndStatus(String condominioId, SubscriptionStatus status);
}
