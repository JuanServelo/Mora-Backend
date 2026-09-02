package com.mora.plan.service;

import com.mora.plan.dto.SubscriptionRequestDTO;
import com.mora.plan.dto.SubscriptionResponseDTO;
import com.mora.plan.entity.Plan;
import com.mora.plan.entity.Subscription;
import com.mora.plan.enums.SubscriptionStatus;
import com.mora.plan.mapper.SubscriptionMapper;
import com.mora.plan.repository.PlanRepository;
import com.mora.plan.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final SubscriptionMapper subscriptionMapper;

    @Transactional
    public SubscriptionResponseDTO createSubscription(SubscriptionRequestDTO dto) {
        Plan plan = planRepository.findById(dto.getPlanId())
                .orElseThrow(() -> new RuntimeException("Plano não encontrado."));

        if (!plan.getIsActive()) {
            throw new RuntimeException("Não é possível assinar um plano inativo.");
        }

        // Cancel previous active subscriptions for the same condominium
        subscriptionRepository.findByCondominioIdAndStatus(dto.getCondominioId(), SubscriptionStatus.ACTIVE)
                .ifPresent(existing -> {
                    existing.setStatus(SubscriptionStatus.CANCELED);
                    existing.setEndDate(LocalDateTime.now());
                    subscriptionRepository.save(existing);
                });

        Subscription subscription = Subscription.builder()
                .condominioId(dto.getCondominioId())
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDateTime.now())
                .build();

        subscription = subscriptionRepository.save(subscription);
        return subscriptionMapper.toResponseDto(subscription);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponseDTO> getSubscriptionsByCondominium(String condominioId) {
        return subscriptionRepository.findByCondominioId(condominioId).stream()
                .map(subscriptionMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SubscriptionResponseDTO cancelSubscription(Long id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assinatura não encontrada."));

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new RuntimeException("A assinatura já não está ativa.");
        }

        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setEndDate(LocalDateTime.now());

        return subscriptionMapper.toResponseDto(subscriptionRepository.save(subscription));
    }
}
