package ocorrencias.service;

import lombok.RequiredArgsConstructor;
import ocorrencias.dto.AndamentoRequest;
import ocorrencias.dto.ConclusaoRequest;
import ocorrencias.dto.NovaOrdemServicoRequest;
import ocorrencias.dto.OrdemServicoResponse;
import ocorrencias.exception.DadosInvalidosException;
import ocorrencias.exception.OperacaoInvalidaException;
import ocorrencias.exception.RecursoNaoEncontradoException;
import ocorrencias.model.Fornecedor;
import ocorrencias.model.OrdemServico;
import ocorrencias.model.OrdemServicoAndamento;
import ocorrencias.model.Reclamacao;
import ocorrencias.model.enums.StatusOrdemServico;
import ocorrencias.model.enums.StatusReclamacao;
import ocorrencias.notificacao.NotificacaoEvento;
import ocorrencias.notificacao.NotificacaoPublisher;
import ocorrencias.repository.OrdemServicoRepository;
import ocorrencias.security.Autorizacao;
import ocorrencias.security.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrdemServicoService {

    private static final Logger log = LoggerFactory.getLogger(OrdemServicoService.class);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final OrdemServicoRepository ordens;
    private final ReclamacaoService reclamacaoService;
    private final FornecedorService fornecedorService;
    private final NotificacaoPublisher notificacoes;

    /**
     * O síndico abre a OS — a partir de uma ocorrência ou avulsa (preventiva).
     *
     * Vinda de ocorrência, as duas ficam vinculadas, a abertura entra no
     * histórico da ocorrência e o morador que a registrou é notificado.
     */
    public OrdemServicoResponse abrir(NovaOrdemServicoRequest req) {
        Autorizacao.exigirSindico("abrir ordens de serviço");
        JwtClaims eu = Autorizacao.atual();
        String condominioId = Autorizacao.exigirCondominio();

        if (req.prazo().isBefore(hoje())) {
            throw new DadosInvalidosException("O prazo não pode estar no passado.");
        }

        Fornecedor fornecedor = null;
        if (req.fornecedorId() != null) {
            fornecedor = fornecedorService.doCondominio(req.fornecedorId());
            if (!fornecedor.isAtivo()) {
                throw new OperacaoInvalidaException("Este fornecedor está inativo.");
            }
        }

        String responsavel = vazioParaNulo(req.responsavel());
        if (responsavel == null && fornecedor != null) responsavel = fornecedor.getNome();
        if (responsavel == null) {
            throw new DadosInvalidosException("Informe o responsável ou escolha um fornecedor.");
        }

        Reclamacao origem = null;
        if (req.reclamacaoId() != null) {
            origem = reclamacaoService.buscar(req.reclamacaoId());
            if (!origem.getCondominioId().equals(condominioId)) throw ReclamacaoService.naoEncontrada();
            if (origem.getStatus() == StatusReclamacao.RESOLVIDO) {
                throw new OperacaoInvalidaException("A ocorrência já está resolvida. Reabra-a antes de abrir uma OS.");
            }
        }

        OrdemServico os = new OrdemServico();
        os.setCondominioId(condominioId);
        os.setNumero(Codigos.gerarUnico("OS", ordens::existsByNumero));
        os.setDescricao(req.descricao().trim());
        os.setPrioridade(req.prioridade());
        os.setPrazo(req.prazo());
        os.setResponsavel(responsavel);
        os.setFornecedor(fornecedor);
        os.setReclamacao(origem);
        os.setStatus(StatusOrdemServico.ABERTA);
        os.setAbertaPorId(eu.authUserId());
        os.setAbertaPorEmail(eu.email());
        os.adicionarAndamento(andamento(eu,
                origem == null ? "OS aberta como manutenção preventiva." : "OS aberta a partir da ocorrência " + origem.getProtocolo() + ".",
                null, StatusOrdemServico.ABERTA));
        ordens.saveAndFlush(os);

        if (origem != null) {
            // Uma ocorrência que vira OS está, por definição, sendo tratada.
            StatusReclamacao anterior = origem.getStatus();
            boolean avanca = anterior == StatusReclamacao.PENDENTE;
            String texto = "Ordem de serviço " + os.getNumero() + " aberta para atender esta ocorrência. Prazo: "
                    + os.getPrazo().format(DATA) + ".";
            reclamacaoService.registrarInteracao(origem, eu, texto,
                    avanca ? anterior : null, avanca ? StatusReclamacao.EM_ANALISE : null);
            if (avanca) reclamacaoService.mudarStatus(origem, StatusReclamacao.EM_ANALISE);

            reclamacaoService.notificarMorador(origem, eu, NotificacaoEvento.ORDEM_SERVICO_ABERTA,
                    "Ordem de serviço aberta para a ocorrência " + origem.getProtocolo(),
                    texto,
                    "os:" + os.getId());
        }

        return OrdemServicoResponse.detalhe(os);
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoResponse> listar(String condominioPedido, StatusOrdemServico status) {
        Autorizacao.exigirVisaoDeGestao("consultar ordens de serviço");
        String condominioId = Autorizacao.condominioDeLeitura(condominioPedido);
        return ordens.listar(condominioId, status).stream().map(OrdemServicoResponse::resumo).toList();
    }

    @Transactional(readOnly = true)
    public OrdemServicoResponse detalhar(Long id) {
        Autorizacao.exigirVisaoDeGestao("consultar ordens de serviço");
        return OrdemServicoResponse.detalhe(buscar(id));
    }

    /**
     * Registro de andamento. O primeiro tira a OS de `ABERTA` e a põe em
     * execução; uma OS atrasada continua atrasada até ser concluída.
     */
    public OrdemServicoResponse registrarAndamento(Long id, AndamentoRequest req) {
        Autorizacao.exigirSindico("registrar andamento de ordens de serviço");
        JwtClaims eu = Autorizacao.atual();
        OrdemServico os = buscarParaEscrita(id);

        StatusOrdemServico anterior = os.getStatus();
        boolean inicia = anterior == StatusOrdemServico.ABERTA;
        os.adicionarAndamento(andamento(eu, req.mensagem().trim(),
                inicia ? anterior : null, inicia ? StatusOrdemServico.EM_EXECUCAO : null));
        if (inicia) os.setStatus(StatusOrdemServico.EM_EXECUCAO);

        return OrdemServicoResponse.detalhe(ordens.saveAndFlush(os));
    }

    /** Encerramento com custo e observações: passa a `CONCLUIDA`. */
    public OrdemServicoResponse concluir(Long id, ConclusaoRequest req) {
        Autorizacao.exigirSindico("concluir ordens de serviço");
        JwtClaims eu = Autorizacao.atual();
        OrdemServico os = buscarParaEscrita(id);

        StatusOrdemServico anterior = os.getStatus();
        os.setCusto(req.custo());
        os.setObservacoes(req.observacoes().trim());
        os.setConcluidaEm(LocalDateTime.now());
        os.setStatus(StatusOrdemServico.CONCLUIDA);
        os.adicionarAndamento(andamento(eu, "OS concluída. " + req.observacoes().trim(),
                anterior, StatusOrdemServico.CONCLUIDA));

        return OrdemServicoResponse.detalhe(ordens.saveAndFlush(os));
    }

    /**
     * Marca como `ATRASADA` toda OS com prazo vencido sem conclusão e avisa o
     * síndico que a abriu. Chamado pelo job diário.
     *
     * Prazo vencido é prazo **antes de hoje**: no próprio dia do prazo a OS
     * ainda está em dia.
     */
    public int marcarAtrasadas() {
        List<OrdemServico> vencidas = ordens.findByStatusInAndPrazoBefore(
                List.of(StatusOrdemServico.ABERTA, StatusOrdemServico.EM_EXECUCAO), hoje());

        for (OrdemServico os : vencidas) {
            StatusOrdemServico anterior = os.getStatus();
            os.setStatus(StatusOrdemServico.ATRASADA);

            OrdemServicoAndamento a = new OrdemServicoAndamento();
            a.setMensagem("Prazo vencido em " + os.getPrazo().format(DATA) + " sem conclusão.");
            a.setStatusAnterior(anterior);
            a.setStatusNovo(StatusOrdemServico.ATRASADA);
            os.adicionarAndamento(a);

            notificacoes.publicar(new NotificacaoEvento(
                    os.getCondominioId(),
                    os.getAbertaPorId(),
                    NotificacaoEvento.ORIGEM,
                    NotificacaoEvento.ORDEM_SERVICO_ATRASADA,
                    "Ordem de serviço " + os.getNumero() + " atrasada",
                    "O prazo venceu em " + os.getPrazo().format(DATA) + " e a OS não foi concluída. Responsável: "
                            + os.getResponsavel() + ".",
                    Map.of("ordemServicoId", os.getId(), "numero", os.getNumero()),
                    "ocorrencias:os:" + os.getId() + ":atrasada"
            ));
        }
        ordens.saveAll(vencidas);

        if (!vencidas.isEmpty()) log.info("{} ordem(ns) de serviço marcada(s) como atrasada(s)", vencidas.size());
        return vencidas.size();
    }

    private OrdemServico buscar(Long id) {
        OrdemServico os = ordens.findDetalhadaById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de serviço não encontrada."));
        if (!Autorizacao.gestaoAlcanca(os.getCondominioId())) {
            throw new RecursoNaoEncontradoException("Ordem de serviço não encontrada.");
        }
        return os;
    }

    private OrdemServico buscarParaEscrita(Long id) {
        OrdemServico os = buscar(id);
        if (os.getStatus() == StatusOrdemServico.CONCLUIDA) {
            throw new OperacaoInvalidaException("Esta ordem de serviço já foi concluída.");
        }
        return os;
    }

    private static OrdemServicoAndamento andamento(JwtClaims autor, String mensagem,
                                                   StatusOrdemServico anterior, StatusOrdemServico novo) {
        OrdemServicoAndamento a = new OrdemServicoAndamento();
        a.setAutorId(autor.authUserId());
        a.setAutorEmail(autor.email());
        a.setMensagem(mensagem);
        a.setStatusAnterior(anterior);
        a.setStatusNovo(novo);
        return a;
    }

    private static LocalDate hoje() {
        return LocalDate.now(EstatisticasReclamacoes.FUSO);
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
