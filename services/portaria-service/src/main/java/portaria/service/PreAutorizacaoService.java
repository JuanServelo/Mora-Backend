package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portaria.dto.PreAutorizacaoRequestDTO;
import portaria.dto.atendimento.AtendimentoRegistroDTO;
import portaria.dto.atendimento.AtendimentoResponseDTO;
import portaria.dto.atendimento.AtendimentoVeiculoDTO;
import portaria.exception.AcessoNegadoException;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.PreAutorizacao;
import portaria.model.enums.StatusPreAutorizacao;
import portaria.model.enums.TipoVisita;
import portaria.repository.PreAutorizacaoRepository;
import portaria.repository.VagaRepository;
import portaria.security.CondominioUtils;
import portaria.util.CpfUtils;
import portaria.util.PlacaUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PreAutorizacaoService {

    private final PreAutorizacaoRepository repository;
    private final VagaRepository vagaRepository;
    private final portaria.repository.VisitanteRepository visitanteRepository;
    private final AtendimentoService atendimentoService;

    public PreAutorizacao cadastrar(String moradorId, String unidadeId, PreAutorizacaoRequestDTO request) {
        if (request.nomeVisitante() == null || request.nomeVisitante().isBlank()) {
            throw new OperacaoInvalidaException("Informe o nome do visitante.");
        }
        // Mesma validação do atendimento: a pré-liberação precisa produzir um
        // cadastro que a portaria consiga usar sem refazer conferência.
        String cpfNorm = CpfUtils.normalizarEValidar(request.cpfVisitante());
        if (request.validadeInicio() == null || request.validadeFim() == null) {
            throw new OperacaoInvalidaException("Informe o período de validade.");
        }
        if (request.validadeFim().isBefore(request.validadeInicio())) {
            throw new OperacaoInvalidaException("Data fim não pode ser anterior à data de início");
        }
        if (request.validadeFim().isBefore(LocalDate.now())) {
            throw new OperacaoInvalidaException("Data fim não pode ser no passado");
        }

        // Terceiro é pré-liberado pelo síndico, que não tem unidade.
        boolean ehTerceiro = request.tipoPessoa() == TipoVisita.SERVICO;
        if (ehTerceiro && (request.empresa() == null || request.empresa().isBlank())) {
            throw new OperacaoInvalidaException("Informe a empresa ou prestador do terceiro.");
        }

        String placaNorm = null;
        if (request.placaVeiculo() != null && !request.placaVeiculo().isBlank()) {
            placaNorm = PlacaUtils.normalizarEValidar(request.placaVeiculo());
            // Sem unidade não há vaga a conferir: a checagem é do fluxo do morador.
            if (unidadeId != null) exigirVagaNaUnidade(unidadeId);
        }

        PreAutorizacao pa = new PreAutorizacao();
        pa.setMoradorId(moradorId);
        pa.setUnidadeId(unidadeId);
        pa.setCondominioId(CondominioUtils.condominioIdEfetivo());
        pa.setNomeVisitante(request.nomeVisitante().trim());
        pa.setCpfVisitante(cpfNorm);
        pa.setTelefoneVisitante(request.telefoneVisitante());
        // Reaproveita a pessoa já cadastrada sem devolver nada dela ao morador:
        // o que ele digitou permanece, e eventual divergência de nome fica para
        // a portaria conferir na entrada (RN-06).
        visitanteRepository.findByCondominioIdAndCpf(pa.getCondominioId(), cpfNorm)
                .ifPresent(v -> pa.setVisitanteId(v.getId()));
        pa.setTipoPessoa(ehTerceiro ? TipoVisita.SERVICO : TipoVisita.VISITA);
        pa.setEmpresa(request.empresa());
        pa.setPlacaVeiculo(placaNorm);
        pa.setModeloVeiculo(request.modeloVeiculo());
        pa.setCorVeiculo(request.corVeiculo());
        pa.setValidadeInicio(request.validadeInicio());
        pa.setValidadeFim(request.validadeFim());
        pa.setObservacoes(request.observacoes());
        pa.setStatus(StatusPreAutorizacao.AGUARDANDO);

        return repository.save(pa);
    }

    /** RN-08: a pré-liberação de veículo exige vaga cadastrada na unidade. */
    private void exigirVagaNaUnidade(String unidadeId) {
        if (vagasDaUnidade(unidadeId).isEmpty()) {
            throw new OperacaoInvalidaException(
                    "Sua unidade não possui vagas cadastradas. Não é possível pré-liberar veículos de visitantes.");
        }
    }

    private List<portaria.model.Vaga> vagasDaUnidade(String unidadeId) {
        try {
            return vagaRepository.findByApartamentoId(UUID.fromString(unidadeId));
        } catch (IllegalArgumentException e) {
            throw new OperacaoInvalidaException("Unidade inválida.");
        }
    }

    /** Cancelamento é do morador: só a própria unidade, e só se ainda não foi usada. */
    public void cancelar(String id, String unidadeId) {
        PreAutorizacao pa = buscarPorId(id);
        exigirMesmaUnidade(pa, unidadeId);
        StatusPreAutorizacao efetivo = pa.statusEfetivo();
        if (efetivo == StatusPreAutorizacao.UTILIZADA) {
            throw new OperacaoInvalidaException("Esta pré-liberação já foi utilizada e não pode ser cancelada.");
        }
        if (efetivo == StatusPreAutorizacao.CANCELADA) {
            throw new OperacaoInvalidaException("Esta pré-liberação já está cancelada.");
        }
        pa.setStatus(StatusPreAutorizacao.CANCELADA);
        pa.setAtualizadoEm(LocalDateTime.now());
        repository.save(pa);
    }

    /** Cancelamento por quem criou — usado pelo síndico, que não tem unidade. */
    public void cancelarComoAutor(String id, String autorId) {
        PreAutorizacao pa = buscarPorId(id);
        if (pa.getMoradorId() == null || !pa.getMoradorId().equals(autorId)) {
            throw new AcessoNegadoException("Você só pode cancelar as pré-liberações que criou.");
        }
        StatusPreAutorizacao efetivo = pa.statusEfetivo();
        if (efetivo == StatusPreAutorizacao.UTILIZADA) {
            throw new OperacaoInvalidaException("Esta pré-liberação já foi utilizada e não pode ser cancelada.");
        }
        if (efetivo == StatusPreAutorizacao.CANCELADA) {
            throw new OperacaoInvalidaException("Esta pré-liberação já está cancelada.");
        }
        pa.setStatus(StatusPreAutorizacao.CANCELADA);
        pa.setAtualizadoEm(LocalDateTime.now());
        repository.save(pa);
    }

    /** Revogação usada pelo fluxo antigo (sem escopo de unidade). */
    public void revogar(String id) {
        PreAutorizacao pa = buscarPorId(id);
        pa.setStatus(StatusPreAutorizacao.CANCELADA);
        pa.setAtualizadoEm(LocalDateTime.now());
        repository.save(pa);
    }

    /**
     * Registra a entrada a partir de uma pré-liberação, em uma só transação.
     *
     * Reaproveita o mesmo caminho do atendimento em vez de duplicar as regras:
     * a pessoa é resolvida por CPF (reaproveitando cadastro), o veículo entra
     * junto quando houver placa, e a vaga é conferida agora — não na criação.
     *
     * Se qualquer etapa falhar (vaga ocupada, por exemplo), tudo é revertido e
     * a pré-liberação continua AGUARDANDO, válida para nova tentativa.
     *
     * @param incluirVeiculo permite a entrada a pé de um visitante que fora
     *                       pré-liberado com carro — o caso de chegar sem vaga.
     */
    public AtendimentoResponseDTO registrarEntrada(String id, boolean incluirVeiculo, String vagaId) {
        PreAutorizacao pa = buscarPorId(id);
        if (!pa.utilizavelHoje()) {
            throw new OperacaoInvalidaException(
                    "Esta pré-liberação não está disponível (" + pa.statusEfetivo() + ").");
        }

        // Terceiro entra como SERVICO, que não tem unidade visitada; visitante
        // entra como VISITA, para a unidade responsável.
        boolean ehTerceiro = pa.getTipoPessoa() == TipoVisita.SERVICO;

        AtendimentoRegistroDTO dto = new AtendimentoRegistroDTO();
        dto.setTipoVisita(ehTerceiro ? "SERVICO" : "VISITA");
        dto.setNome(pa.getNomeVisitante());
        dto.setDocumento(pa.getCpfVisitante());
        dto.setTelefone(pa.getTelefoneVisitante());
        dto.setEmpresa(pa.getEmpresa());
        dto.setApartamentoId(ehTerceiro ? null : pa.getUnidadeId());
        // Quem pré-liberou responde pela visita — morador ou síndico.
        dto.setMoradorResponsavelId(ehTerceiro ? null : pa.getMoradorId());
        dto.setObs(pa.getObservacoes());

        if (incluirVeiculo && pa.getPlacaVeiculo() != null && !pa.getPlacaVeiculo().isBlank()) {
            AtendimentoVeiculoDTO veiculo = new AtendimentoVeiculoDTO();
            veiculo.setPlaca(pa.getPlacaVeiculo());
            veiculo.setModelo(pa.getModeloVeiculo());
            veiculo.setCor(pa.getCorVeiculo());
            veiculo.setVagaId(vagaId);
            dto.setVeiculo(veiculo);
        }

        AtendimentoResponseDTO resposta = atendimentoService.registrar(dto);

        pa.setStatus(StatusPreAutorizacao.UTILIZADA);
        pa.setUtilizadaEm(LocalDateTime.now());
        pa.setAtualizadoEm(LocalDateTime.now());
        repository.save(pa);

        return resposta;
    }

    /** Marca como usada na entrada; uma pré-liberação não se reaproveita (RN-08). */
    public PreAutorizacao marcarUtilizada(String id) {
        PreAutorizacao pa = buscarPorId(id);
        if (!pa.utilizavelHoje()) {
            throw new OperacaoInvalidaException(
                    "Esta pré-liberação não está mais disponível (" + pa.statusEfetivo() + ").");
        }
        pa.setStatus(StatusPreAutorizacao.UTILIZADA);
        pa.setUtilizadaEm(LocalDateTime.now());
        pa.setAtualizadoEm(LocalDateTime.now());
        return repository.save(pa);
    }

    private void exigirMesmaUnidade(PreAutorizacao pa, String unidadeId) {
        if (pa.getUnidadeId() == null || !pa.getUnidadeId().equals(unidadeId)) {
            throw new AcessoNegadoException("Esta pré-liberação não pertence à sua unidade.");
        }
    }

    @Transactional(readOnly = true)
    public PreAutorizacao buscarPorId(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pré-autorização não encontrada com id: " + id));
    }

    @Transactional(readOnly = true)
    public List<PreAutorizacao> listarDoMorador(String moradorId) {
        return repository.findByMoradorId(moradorId);
    }

    /**
     * A tela do morador mostra o que a unidade inteira pré-liberou.
     * RN-07: ativas primeiro, depois por validade mais recente.
     */
    @Transactional(readOnly = true)
    public List<PreAutorizacao> listarDaUnidade(String unidadeId) {
        return repository.findByUnidadeId(unidadeId).stream()
                .sorted(Comparator
                        .comparing((PreAutorizacao p) -> p.statusEfetivo() != StatusPreAutorizacao.AGUARDANDO)
                        .thenComparing(PreAutorizacao::getValidadeFim, Comparator.reverseOrder()))
                .toList();
    }

    /** Atendimento: autorização ativa para o CPF informado (RN-08). */
    @Transactional(readOnly = true)
    public List<PreAutorizacao> buscarPorCpf(String condominioId, String cpf) {
        String norm = CpfUtils.normalizar(cpf);
        if (norm == null || norm.isBlank()) return List.of();
        return repository.buscarPorCpf(condominioId, LocalDate.now(), norm);
    }

    @Transactional(readOnly = true)
    public List<PreAutorizacao> listarAtivasHoje(String condominioId) {
        if (condominioId == null) return repository.findTodasAtivasHoje(LocalDate.now());
        return repository.findAtivasHoje(condominioId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<PreAutorizacao> buscarPorNomeOuCpf(String condominioId, String termo) {
        if (condominioId == null) {
            return repository.findTodasAtivasHoje(LocalDate.now()).stream()
                    .filter(p -> p.getNomeVisitante().toLowerCase().contains(termo.toLowerCase())
                            || termo.equals(p.getCpfVisitante()))
                    .toList();
        }
        return repository.buscarPorNomeOuCpf(condominioId, LocalDate.now(), termo);
    }

    /** Atendimento: localizar autorização ativa pela placa informada no portão. */
    @Transactional(readOnly = true)
    public List<PreAutorizacao> buscarPorPlaca(String condominioId, String placa) {
        String norm = PlacaUtils.normalizar(placa);
        if (norm == null || norm.isBlank()) return List.of();
        return repository.buscarPorPlaca(condominioId, LocalDate.now(), norm);
    }
}
