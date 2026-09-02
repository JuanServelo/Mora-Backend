package com.mora.plan.repository;

import com.mora.plan.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
