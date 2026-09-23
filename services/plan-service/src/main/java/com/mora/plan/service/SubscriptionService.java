package com.mora.plan.service;

import com.mora.plan.dto.ChangePlanRequestDTO;
import com.mora.plan.dto.SubscriptionRequestDTO;
import com.mora.plan.dto.SubscriptionResponseDTO;
import com.mora.plan.entity.Plan;
import com.mora.plan.entity.Subscription;
import com.mora.plan.enums.SubscriptionStatus;
import com.mora.plan.mapper.SubscriptionMapper;
import com.mora.plan.repository.PlanRepository;
import com.mora.plan.repository.SubscriptionRepository;
import com.mora.plan.security.AuthContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final SubscriptionMapper subscriptionMapper;

    /**
     * Cria uma assinatura para um condomínio.
     */
    @Transactional
    public SubscriptionResponseDTO createSubscription(SubscriptionRequestDTO dto) {
        // Verificar se o condomínio já possui assinatura ativa
        if (subscriptionRepository.existsByCondominioIdAndStatus(dto.getCondominioId(), SubscriptionStatus.ACTIVE)) {
            throw new IllegalArgumentException("Este condomínio já possui uma assinatura ativa.");
        }

        Plan plan = planRepository.findById(dto.getPlanId())
                .orElseThrow(() -> new RuntimeException("Plano não encontrado com o ID: " + dto.getPlanId()));

        if (!plan.getIsActive()) {
            throw new IllegalArgumentException("O plano selecionado não está ativo.");
        }

        String email = getEmailFromContext();

        Subscription subscription = Subscription.builder()
                .condominioId(dto.getCondominioId())
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .createdBy(email)
                .updatedBy(email)
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Assinatura criada: condomínio={}, plano={}, por={}", dto.getCondominioId(), plan.getName(), email);

        return subscriptionMapper.toResponseDto(saved);
    }

    /**
     * Lista todas as assinaturas.
     */
    @Transactional(readOnly = true)
    public List<SubscriptionResponseDTO> listAll() {
        return subscriptionRepository.findAll().stream()
                .map(subscriptionMapper::toResponseDto)
                .toList();
    }

    /**
     * Busca a assinatura ativa de um condomínio.
     */
    @Transactional(readOnly = true)
    public SubscriptionResponseDTO getByCondominioId(String condominioId) {
        Subscription subscription = subscriptionRepository
                .findByCondominioIdAndStatus(condominioId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Assinatura ativa não encontrada para o condomínio: " + condominioId));

        return subscriptionMapper.toResponseDto(subscription);
    }

    /**
     * Retorna apenas os slugs dos módulos ativos do condomínio.
     * Retorna lista vazia se não houver assinatura ativa.
     */
    @Transactional(readOnly = true)
    public List<String> getModulesByCondominioId(String condominioId) {
        return subscriptionRepository
                .findByCondominioIdAndStatus(condominioId, SubscriptionStatus.ACTIVE)
                .map(sub -> sub.getPlan().getActiveModules())
                .orElse(List.of());
    }

    /**
     * Suspende uma assinatura.
     */
    @Transactional
    public SubscriptionResponseDTO suspend(Long id) {
        Subscription subscription = findById(id);

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Apenas assinaturas ativas podem ser suspensas.");
        }

        String email = getEmailFromContext();
        subscription.setStatus(SubscriptionStatus.SUSPENDED);
        subscription.setUpdatedBy(email);

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Assinatura {} suspensa para condomínio={}, por={}", id, subscription.getCondominioId(), email);

        return subscriptionMapper.toResponseDto(saved);
    }

    /**
     * Cancela uma assinatura.
     */
    @Transactional
    public SubscriptionResponseDTO cancel(Long id) {
        Subscription subscription = findById(id);

        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new IllegalStateException("Esta assinatura já está cancelada.");
        }

        String email = getEmailFromContext();
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setUpdatedBy(email);

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Assinatura {} cancelada para condomínio={}, por={}", id, subscription.getCondominioId(), email);

        return subscriptionMapper.toResponseDto(saved);
    }

    /**
     * Troca o plano de um condomínio: cancela a assinatura atual e cria uma nova.
     */
    @Transactional
    public SubscriptionResponseDTO changePlan(String condominioId, ChangePlanRequestDTO dto) {
        // Cancelar assinatura ativa existente (se houver)
        subscriptionRepository.findByCondominioIdAndStatus(condominioId, SubscriptionStatus.ACTIVE)
                .ifPresent(existing -> {
                    existing.setStatus(SubscriptionStatus.CANCELLED);
                    existing.setUpdatedBy(getEmailFromContext());
                    subscriptionRepository.save(existing);
                    log.info("Assinatura anterior cancelada para troca de plano: condomínio={}", condominioId);
                });

        Plan newPlan = planRepository.findById(dto.getNewPlanId())
                .orElseThrow(() -> new RuntimeException("Plano não encontrado com o ID: " + dto.getNewPlanId()));

        if (!newPlan.getIsActive()) {
            throw new IllegalArgumentException("O plano selecionado não está ativo.");
        }

        String email = getEmailFromContext();

        Subscription subscription = Subscription.builder()
                .condominioId(condominioId)
                .plan(newPlan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDate.now())
                .createdBy(email)
                .updatedBy(email)
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Plano trocado: condomínio={}, novoPlano={}, por={}", condominioId, newPlan.getName(), email);

        return subscriptionMapper.toResponseDto(saved);
    }

    private Subscription findById(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assinatura não encontrada com o ID: " + id));
    }

    private String getEmailFromContext() {
        var claims = AuthContext.get();
        return claims != null ? claims.email() : "sistema";
    }
}
