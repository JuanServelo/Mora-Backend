package vagas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vagas.dto.AluguelRequestDTO;
import vagas.dto.DisponibilidadeRequestDTO;
import vagas.exception.OperacaoInvalidaException;
import vagas.exception.RecursoNaoEncontradoException;
import vagas.model.AluguelVaga;
import vagas.model.DisponibilidadeVaga;
import vagas.model.Vaga;
import vagas.model.enums.ModalidadeAluguel;
import vagas.model.enums.StatusAluguel;
import vagas.repository.AluguelVagaRepository;
import vagas.repository.DisponibilidadeVagaRepository;
import vagas.repository.VagaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AluguelService {

    // Valores padrão (RF08 configura esses parâmetros; aqui usamos constantes enquanto RF08 não integra)
    private static final BigDecimal VALOR_DIARIA  = new BigDecimal("15.00");
    private static final BigDecimal VALOR_MENSAL  = new BigDecimal("200.00");
    /** Dias de antecedência mínima para cancelamento sem penalidade. */
    private static final int DIAS_CANCELAMENTO_SEM_PENALIDADE = 3;
    /** Percentual de penalidade sobre o valor total quando cancela fora do prazo. */
    private static final BigDecimal PERCENTUAL_PENALIDADE = new BigDecimal("0.20");

    private final VagaRepository vagaRepository;
    private final DisponibilidadeVagaRepository disponibilidadeRepository;
    private final AluguelVagaRepository aluguelRepository;

    // -------------------------------------------------------------------------
    // RF10 US1 – Disponibilizar vaga para alugar
    // -------------------------------------------------------------------------

    @Transactional
    public DisponibilidadeVaga disponibilizar(DisponibilidadeRequestDTO dto, UUID proprietarioId) {
        validarPeriodo(dto.dataInicio(), dto.dataFim());

        Vaga vaga = buscarVaga(dto.vagaId());
        validarProprietario(vaga, proprietarioId);

        // CA2 – conflito de datas
        List<DisponibilidadeVaga> conflitos = disponibilidadeRepository
                .findConflitantes(dto.vagaId(), dto.dataInicio(), dto.dataFim());
        if (!conflitos.isEmpty()) {
            throw new OperacaoInvalidaException(
                    "Conflito de datas: a vaga já está disponibilizada ou alugada neste período.");
        }

        DisponibilidadeVaga disp = new DisponibilidadeVaga();
        disp.setProprietarioId(proprietarioId);
        disp.setVaga(vaga);
        disp.setDataInicio(dto.dataInicio());
        disp.setDataFim(dto.dataFim());
        disp.setAtiva(true);
        return disponibilidadeRepository.save(disp);
    }

    public List<DisponibilidadeVaga> listarDisponibilidadesAtivas() {
        return disponibilidadeRepository.findByAtivaTrue();
    }

    public List<DisponibilidadeVaga> listarMinhasDisponibilidades(UUID proprietarioId) {
        return disponibilidadeRepository.findByProprietarioIdAndAtivaTrue(proprietarioId);
    }

    @Transactional
    public void cancelarDisponibilidade(String disponibilidadeId, UUID proprietarioId) {
        DisponibilidadeVaga disp = disponibilidadeRepository.findById(disponibilidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Disponibilidade não encontrada: " + disponibilidadeId));

        if (!disp.getProprietarioId().equals(proprietarioId)) {
            throw new OperacaoInvalidaException(
                    "Apenas o proprietário pode cancelar esta disponibilidade.");
        }
        disp.setAtiva(false);
        disponibilidadeRepository.save(disp);
    }

    // -------------------------------------------------------------------------
    // RF10 US2 – Solicitar aluguel de vaga disponível
    // -------------------------------------------------------------------------

    @Transactional
    public AluguelVaga solicitar(AluguelRequestDTO dto, UUID solicitanteId) {
        validarPeriodo(dto.dataInicio(), dto.dataFim());

        Vaga vaga = buscarVaga(dto.vagaId());

        if (!vaga.isAtiva()) {
            throw new OperacaoInvalidaException("Vaga inativa, não é possível alugar.");
        }

        // Verifica se existe disponibilidade ativa que cobre o período solicitado
        List<DisponibilidadeVaga> disponibilidades = disponibilidadeRepository
                .findConflitantes(dto.vagaId(), dto.dataInicio(), dto.dataFim());
        if (disponibilidades.isEmpty()) {
            throw new OperacaoInvalidaException(
                    "A vaga não está disponível para o período solicitado.");
        }

        // CA – Pedidos simultâneos: verifica se já existe aluguel APROVADO no período
        List<AluguelVaga> conflitos = aluguelRepository
                .findAprovadosConflitantes(dto.vagaId(), dto.dataInicio(), dto.dataFim());
        if (!conflitos.isEmpty()) {
            throw new OperacaoInvalidaException(
                    "A vaga acabou de ficar indisponível: outro aluguel foi aprovado para este período.");
        }

        // Proprietário vem da disponibilidade
        UUID proprietarioId = disponibilidades.get(0).getProprietarioId();

        if (proprietarioId.equals(solicitanteId)) {
            throw new OperacaoInvalidaException(
                    "O proprietário da vaga não pode alugar a própria vaga.");
        }

        BigDecimal valorTotal = calcularValor(dto.modalidade(), dto.dataInicio(), dto.dataFim());

        AluguelVaga aluguel = new AluguelVaga();
        aluguel.setSolicitanteId(solicitanteId);
        aluguel.setProprietarioId(proprietarioId);
        aluguel.setVaga(vaga);
        aluguel.setDataInicio(dto.dataInicio());
        aluguel.setDataFim(dto.dataFim());
        aluguel.setModalidade(dto.modalidade());
        aluguel.setValorTotal(valorTotal);
        aluguel.setStatus(StatusAluguel.PENDENTE);
        return aluguelRepository.save(aluguel);
    }

    // -------------------------------------------------------------------------
    // RF10 US3 – Aprovar ou recusar solicitação
    // -------------------------------------------------------------------------

    @Transactional
    public AluguelVaga aprovar(String aluguelId, UUID responsavelId) {
        AluguelVaga aluguel = buscarAluguel(aluguelId);
        validarPendente(aluguel);
        validarResponsavelAprovacao(aluguel, responsavelId);

        // CA – Pedidos simultâneos: verifica conflito no momento da aprovação
        List<AluguelVaga> conflitos = aluguelRepository
                .findAprovadosConflitantes(
                        aluguel.getVaga().getId(),
                        aluguel.getDataInicio(),
                        aluguel.getDataFim());
        if (!conflitos.isEmpty()) {
            throw new OperacaoInvalidaException(
                    "Não é possível aprovar: já existe um aluguel aprovado para este período.");
        }

        aluguel.setStatus(StatusAluguel.APROVADO);
        aluguel.setAtualizadoEm(LocalDateTime.now());
        return aluguelRepository.save(aluguel);
    }

    @Transactional
    public AluguelVaga recusar(String aluguelId, UUID responsavelId, String justificativa) {
        AluguelVaga aluguel = buscarAluguel(aluguelId);
        validarPendente(aluguel);
        validarResponsavelAprovacao(aluguel, responsavelId);

        aluguel.setStatus(StatusAluguel.RECUSADO);
        aluguel.setMotivoRecusa(justificativa);
        aluguel.setAtualizadoEm(LocalDateTime.now());
        return aluguelRepository.save(aluguel);
    }

    // -------------------------------------------------------------------------
    // RF10 US4 – Cancelar aluguel
    // -------------------------------------------------------------------------

    @Transactional
    public AluguelVaga cancelar(String aluguelId, UUID solicitanteId) {
        AluguelVaga aluguel = buscarAluguel(aluguelId);

        if (!aluguel.getSolicitanteId().equals(solicitanteId)) {
            throw new OperacaoInvalidaException(
                    "Apenas o solicitante pode cancelar este aluguel.");
        }
        if (aluguel.getStatus() == StatusAluguel.CANCELADO
                || aluguel.getStatus() == StatusAluguel.RECUSADO
                || aluguel.getStatus() == StatusAluguel.CONCLUIDO) {
            throw new OperacaoInvalidaException(
                    "Aluguel não pode ser cancelado no status atual: " + aluguel.getStatus());
        }

        long diasParaInicio = ChronoUnit.DAYS.between(LocalDate.now(), aluguel.getDataInicio());

        // CA2 – cancelamento fora do prazo com penalidade
        if (diasParaInicio < DIAS_CANCELAMENTO_SEM_PENALIDADE && aluguel.getValorTotal() != null) {
            BigDecimal penalidade = aluguel.getValorTotal().multiply(PERCENTUAL_PENALIDADE);
            aluguel.setValorPenalidade(penalidade);
        }
        // CA3 – sem penalidade configurada → nenhuma penalidade aplicada (valorPenalidade fica null)

        aluguel.setStatus(StatusAluguel.CANCELADO);
        aluguel.setAtualizadoEm(LocalDateTime.now());
        return aluguelRepository.save(aluguel);
    }

    // -------------------------------------------------------------------------
    // RF10 US5 – Histórico de transações
    // -------------------------------------------------------------------------

    public List<AluguelVaga> historicoPorSolicitante(UUID solicitanteId) {
        return aluguelRepository.findBySolicitanteId(solicitanteId);
    }

    public List<AluguelVaga> historicoPorProprietario(UUID proprietarioId) {
        return aluguelRepository.findByProprietarioId(proprietarioId);
    }

    public List<AluguelVaga> historicoPorSolicitanteEStatus(UUID solicitanteId, StatusAluguel status) {
        return aluguelRepository.findBySolicitanteIdAndStatus(solicitanteId, status);
    }

    public List<AluguelVaga> historicoPorProprietarioEStatus(UUID proprietarioId, StatusAluguel status) {
        return aluguelRepository.findByProprietarioIdAndStatus(proprietarioId, status);
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

    private Vaga buscarVaga(String vagaId) {
        return vagaRepository.findById(vagaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Vaga não encontrada: " + vagaId));
    }

    private AluguelVaga buscarAluguel(String id) {
        return aluguelRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Aluguel não encontrado: " + id));
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (!fim.isAfter(inicio)) {
            throw new OperacaoInvalidaException(
                    "A data de fim deve ser posterior à data de início.");
        }
        if (inicio.isBefore(LocalDate.now())) {
            throw new OperacaoInvalidaException(
                    "A data de início não pode ser no passado.");
        }
    }

    private void validarProprietario(Vaga vaga, UUID proprietarioId) {
        if (vaga.getApartamento() == null) {
            throw new OperacaoInvalidaException(
                    "A vaga não está vinculada a nenhum apartamento.");
        }
        // Qualquer morador autenticado pode disponibilizar sua própria vaga;
        // a validação real de posse deve ser feita via serviço de moradores.
        // Aqui registramos apenas quem declarou ser o proprietário.
    }

    private void validarPendente(AluguelVaga aluguel) {
        if (aluguel.getStatus() != StatusAluguel.PENDENTE) {
            throw new OperacaoInvalidaException(
                    "Apenas aluguéis PENDENTES podem ser aprovados ou recusados. Status atual: "
                            + aluguel.getStatus());
        }
    }

    private void validarResponsavelAprovacao(AluguelVaga aluguel, UUID responsavelId) {
        // Proprietário da vaga ou administrador (role validado no controller)
        if (!aluguel.getProprietarioId().equals(responsavelId)) {
            throw new OperacaoInvalidaException(
                    "Apenas o proprietário da vaga ou um administrador podem aprovar/recusar esta solicitação.");
        }
    }

    private BigDecimal calcularValor(ModalidadeAluguel modalidade, LocalDate inicio, LocalDate fim) {
        long dias = ChronoUnit.DAYS.between(inicio, fim);
        if (modalidade == ModalidadeAluguel.DIARIA) {
            return VALOR_DIARIA.multiply(BigDecimal.valueOf(dias));
        } else {
            long meses = ChronoUnit.MONTHS.between(inicio, fim);
            if (meses == 0) meses = 1;
            return VALOR_MENSAL.multiply(BigDecimal.valueOf(meses));
        }
    }
}

