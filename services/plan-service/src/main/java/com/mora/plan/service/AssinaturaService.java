package com.mora.plan.service;

import com.mora.plan.dto.AssinaturaRequestDTO;
import com.mora.plan.dto.AssinaturaResponseDTO;
import com.mora.plan.entity.Assinatura;
import com.mora.plan.entity.Plan;
import com.mora.plan.enums.StatusAssinatura;
import com.mora.plan.repository.AssinaturaRepository;
import com.mora.plan.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AssinaturaService {

    private final AssinaturaRepository assinaturaRepository;
    private final PlanRepository planRepository;

    @Transactional(readOnly = true)
    public List<AssinaturaResponseDTO> listarTodas() {
        return assinaturaRepository.findAll().stream().map(this::paraDTO).toList();
    }

    @Transactional(readOnly = true)
    public AssinaturaResponseDTO buscarVigentePorCondominio(String condominioId) {
        Assinatura a = assinaturaRepository
                .findByCondominioIdAndStatus(condominioId, StatusAssinatura.ATIVA)
                // Ausencia nao e requisicao invalida: condominio sem assinatura ativa
                // e um estado legitimo, e o chamador precisa distinguir isso de erro
                // dele. NoSuchElementException vira 404 no GlobalExceptionHandler.
                .orElseThrow(() -> new NoSuchElementException(
                        "Nenhuma assinatura ativa para o condomínio " + condominioId));
        return paraDTO(a);
    }

    @Transactional(readOnly = true)
    public List<AssinaturaResponseDTO> historicoDoCondominio(String condominioId) {
        return assinaturaRepository.findByCondominioIdOrderByCreatedAtDesc(condominioId)
                .stream().map(this::paraDTO).toList();
    }

    @Transactional
    public AssinaturaResponseDTO criar(AssinaturaRequestDTO dto) {
        Plan plano = planRepository.findById(dto.getPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado"));

        // Um condomínio não pode ter duas assinaturas ativas ao mesmo tempo:
        // a anterior é encerrada antes de a nova entrar.
        //
        // saveAndFlush, e não save: o Hibernate ordena INSERT antes de UPDATE
        // dentro do mesmo flush. Com save(), a nova linha era inserida enquanto
        // a anterior ainda estava ATIVA, e o indice unico parcial recusava —
        // trocar de plano falhava sempre com duplicate key.
        assinaturaRepository
                .findByCondominioIdAndStatus(dto.getCondominioId(), StatusAssinatura.ATIVA)
                .ifPresent(atual -> {
                    atual.setStatus(StatusAssinatura.CANCELADA);
                    atual.setVigenciaFim(dto.getVigenciaInicio() != null
                            ? dto.getVigenciaInicio()
                            : LocalDate.now());
                    assinaturaRepository.saveAndFlush(atual);
                });

        Assinatura nova = Assinatura.builder()
                .condominioId(dto.getCondominioId())
                .plan(plano)
                .vigenciaInicio(dto.getVigenciaInicio() != null ? dto.getVigenciaInicio() : LocalDate.now())
                .vigenciaFim(dto.getVigenciaFim())
                .status(dto.getStatus() != null ? dto.getStatus() : StatusAssinatura.ATIVA)
                .observacao(dto.getObservacao())
                .build();

        return paraDTO(assinaturaRepository.save(nova));
    }

    @Transactional
    public AssinaturaResponseDTO alterarStatus(Long id, StatusAssinatura status) {
        Assinatura a = assinaturaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assinatura não encontrada"));
        a.setStatus(status);
        if (status != StatusAssinatura.ATIVA && a.getVigenciaFim() == null) {
            a.setVigenciaFim(LocalDate.now());
        }
        return paraDTO(assinaturaRepository.save(a));
    }

    private AssinaturaResponseDTO paraDTO(Assinatura a) {
        Plan p = a.getPlan();
        return AssinaturaResponseDTO.builder()
                .id(a.getId())
                .condominioId(a.getCondominioId())
                .status(a.getStatus())
                .vigenciaInicio(a.getVigenciaInicio())
                .vigenciaFim(a.getVigenciaFim())
                .vigente(a.estaVigente())
                .planId(p.getId())
                .planNome(p.getName())
                .mensalidade(p.getMonthlyPrice())
                .maxCondominios(p.getMaxCondominiums())
                .maxUsuariosPorCondominio(p.getMaxUsersPerCondominium())
                .modulosAtivos(p.getActiveModules())
                .observacao(a.getObservacao())
                .build();
    }
}
