package com.mora.plan.service;

import com.mora.plan.dto.*;
import com.mora.plan.entity.Assinatura;
import com.mora.plan.enums.StatusAssinatura;
import com.mora.plan.repository.AssinaturaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

/** Compatibility API: both languages operate on tb_assinaturas. */
@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final AssinaturaService assinaturas;
    private final AssinaturaRepository repository;

    @Transactional
    public SubscriptionResponseDTO createSubscription(SubscriptionRequestDTO dto) {
        if (repository.findByCondominioIdAndStatus(dto.getCondominioId(), StatusAssinatura.ATIVA).isPresent()) {
            throw new IllegalArgumentException("Este condomínio já possui uma assinatura ativa.");
        }
        return create(dto.getCondominioId(), dto.getPlanId(), dto.getStartDate(), dto.getEndDate());
    }

    private SubscriptionResponseDTO create(String condominio, Long plan, LocalDate start, LocalDate end) {
        AssinaturaRequestDTO request = new AssinaturaRequestDTO();
        request.setCondominioId(condominio);
        request.setPlanId(plan);
        request.setVigenciaInicio(start);
        request.setVigenciaFim(end);
        return response(find(assinaturas.criar(request).getId()));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponseDTO> listAll() {
        return repository.findAll().stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionResponseDTO getByCondominioId(String id) {
        return response(find(assinaturas.buscarVigentePorCondominio(id).getId()));
    }

    @Transactional(readOnly = true)
    public List<String> getModulesByCondominioId(String id) {
        return assinaturas.modulosPermitidos(id);
    }

    @Transactional
    public SubscriptionResponseDTO suspend(Long id) {
        if (find(id).getStatus() != StatusAssinatura.ATIVA) {
            throw new IllegalStateException("Apenas assinaturas ativas podem ser suspensas.");
        }
        assinaturas.alterarStatus(id, StatusAssinatura.SUSPENSA);
        return response(find(id));
    }

    @Transactional
    public SubscriptionResponseDTO cancel(Long id) {
        assinaturas.alterarStatus(id, StatusAssinatura.CANCELADA);
        return response(find(id));
    }

    @Transactional
    public SubscriptionResponseDTO changePlan(String id, ChangePlanRequestDTO dto) {
        return create(id, dto.getNewPlanId(), LocalDate.now(), null);
    }

    private Assinatura find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("Assinatura não encontrada"));
    }

    private SubscriptionResponseDTO response(Assinatura a) {
        String status = switch (a.getStatus()) {
            case ATIVA -> "ACTIVE";
            case SUSPENSA -> "SUSPENDED";
            case CANCELADA -> "CANCELLED";
        };
        return SubscriptionResponseDTO.builder().id(a.getId()).condominioId(a.getCondominioId())
                .planId(a.getPlan().getId()).planName(a.getPlan().getName()).status(status)
                .startDate(a.getVigenciaInicio()).endDate(a.getVigenciaFim())
                .createdBy(a.getCreatedBy()).updatedBy(a.getUpdatedBy())
                .createdAt(a.getCreatedAt()).updatedAt(a.getUpdatedAt()).build();
    }
}
