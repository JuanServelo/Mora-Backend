package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portaria.client.AuthApiClient;
import portaria.dto.reserva.ReservaRequestDTO;
import portaria.exception.AcessoNegadoException;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.AreaComum;
import portaria.model.AreaComumHorario;
import portaria.model.Reserva;
import portaria.model.enums.ModoFuncionamento;
import portaria.model.enums.StatusReserva;
import portaria.model.enums.TipoReserva;
import portaria.repository.ReservaRepository;
import portaria.security.CondominioUtils;
import portaria.security.JwtClaims;
import portaria.util.FuncionamentoUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservaService {

    private static final DateTimeFormatter DT_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Prazo para o síndico analisar antes do início da reserva (RN-05).
     * Passado ele, a pendente expira e o horário volta a ficar livre.
     */
    public static final Duration PRAZO_APROVACAO = Duration.ofHours(24);

    /** Perfis que aprovam reserva — constante nomeada, não check espalhado. */
    private static final List<String> PERFIS_APROVAM =
            List.of("ADMIN_SINDICO", "ADMIN_GERAL");

    private final ReservaRepository reservaRepository;
    private final AreaComunService areaComunService;
    private final FuncionamentoAreaComumService funcionamentoService;
    private final AuthApiClient authApiClient;
    private final portaria.repository.ReservaAuditoriaRepository auditoriaRepository;

    public static boolean podeAprovar(String perfil) {
        return PERFIS_APROVAM.contains(perfil);
    }

    // ─── Criação ─────────────────────────────────────────────────────────────

    public Reserva solicitar(JwtClaims claims, ReservaRequestDTO request) {
        LocalDateTime inicio = request.inicio();
        LocalDateTime fim = request.fim();

        if (inicio == null || fim == null) {
            throw new OperacaoInvalidaException("Informe o início e o fim da reserva.");
        }
        if (!fim.isAfter(inicio)) {
            throw new OperacaoInvalidaException("O fim da reserva deve ser posterior ao início.");
        }
        // RN-03: relógio do servidor — validar só no cliente é contornável.
        if (inicio.isBefore(LocalDateTime.now())) {
            throw new OperacaoInvalidaException("Não é possível reservar em data ou horário passado.");
        }

        AreaComum area = areaComunService.buscarPorId(request.areaComumId());
        exigirCondominioDaArea(area);
        if (!area.isAtivo()) {
            throw new OperacaoInvalidaException("Esta área comum está inativa");
        }
        if (!area.isPodeReservar()) {
            throw new OperacaoInvalidaException("Esta área comum não está disponível para reserva");
        }

        Reserva reserva = new Reserva();
        aplicarResponsavel(claims, request, reserva);

        // Varre as pendentes vencidas antes de checar conflito: uma pendente
        // esquecida já não deveria estar ocupando o horário.
        expirarPendentesVencidas();

        // RN-06: a checagem usa lock pessimista no mesmo espaço, então duas
        // requisições simultâneas não passam as duas antes de qualquer gravar.
        List<Reserva> conflitos = reservaRepository.findConflitantes(area.getId(), inicio, fim, null);
        if (!conflitos.isEmpty()) {
            Reserva c = conflitos.get(0);
            // Período completo com data: uma reserva corrida pode ocupar horas
            // que sequer aparecem na agenda, e só "das 14:00 às 09:00" confundiria.
            throw new OperacaoInvalidaException(
                    "O espaço " + area.getNome() + " está reservado de "
                    + c.getInicio().format(DT_BR) + " até " + c.getFim().format(DT_BR)
                    + " por " + (c.getResponsavelNome() != null ? c.getResponsavelNome() : "outro responsável") + ".");
        }

        // Depois do conflito: um horário ocupado por reserva corrida pode estar
        // fora do funcionamento, e aí o motivo útil é a ocupação, não a janela.
        validarJanelaFuncionamento(area, inicio, fim);

        reserva.setSolicitanteId(claims.authUserId());
        reserva.setAreaComum(area);
        reserva.setCondominioId(area.getCondominioId());
        reserva.setInicio(inicio);
        reserva.setFim(fim);
        reserva.setObservacoes(request.observacoes());

        // RN-04: só nasce pendente onde o espaço exige aprovação — e o síndico
        // não aprova a si mesmo.
        boolean precisaAprovacao = area.isExigeAprovacao() && !podeAprovar(claims.perfil());
        reserva.setStatus(precisaAprovacao ? StatusReserva.PENDENTE : StatusReserva.APROVADA);
        if (!precisaAprovacao) {
            // Confirmação automática: marca quando, mas não quem. Gravar o
            // solicitante aqui faria a tela dizer "aprovado por" ele mesmo,
            // que é justamente o que não aconteceu — ninguém decidiu nada.
            boolean decidiu = area.isExigeAprovacao();
            if (decidiu) {
                reserva.setAprovadoPorId(claims.authUserId());
                reserva.setAprovadoPorNome(claims.exibicao());
            }
            reserva.setAprovadoEm(LocalDateTime.now());
        }

        if (area.getTaxaLocacao() != null && area.getTaxaLocacao() > 0) {
            long dias = Math.max(1, Duration.between(inicio, fim).toDays());
            reserva.setValorTotal(BigDecimal.valueOf(area.getTaxaLocacao() * dias));
        }

        return reservaRepository.save(reserva);
    }

    /**
     * Só as pontas precisam cair dentro do funcionamento: uma reserva de
     * período corrido ocupa o espaço continuamente, inclusive nas faixas
     * fechadas, então validar o intervalo inteiro inviabilizaria o caso.
     */
    private void validarJanelaFuncionamento(AreaComum area, LocalDateTime inicio, LocalDateTime fim) {
        if (area.getModoFuncionamento() != ModoFuncionamento.HORARIOS_DEFINIDOS) return;

        List<AreaComumHorario> horarios = funcionamentoService.horariosDe(area.getId());
        if (!FuncionamentoUtils.permitidoInicioEFim(horarios, inicio, fim)) {
            throw new OperacaoInvalidaException(
                    FuncionamentoUtils.mensagemRecusa(horarios, area.getNome(), inicio, fim));
        }
    }

    /**
     * RN-04/RN-05. Morador nunca escolhe unidade nem tipo: o vínculo vem do
     * token. Porteiro e síndico escolhem, e o perfil do responsável precisa
     * corresponder ao tipo — caso contrário, 400.
     */
    private void aplicarResponsavel(JwtClaims claims, ReservaRequestDTO request, Reserva reserva) {
        boolean operador = ehOperador(claims.perfil());
        TipoReserva tipo = request.tipoReserva() != null ? request.tipoReserva() : TipoReserva.MORADOR;

        if (!operador) {
            if (tipo == TipoReserva.EVENTO_CONDOMINIO) {
                throw new AcessoNegadoException(
                        "Apenas porteiro ou síndico pode criar reserva de evento do condomínio.");
            }
            String unidade = CondominioUtils.unidadeIdObrigatoria();
            AuthApiClient.PessoaUnidade eu = authApiClient.pessoasDaMinhaUnidade().stream()
                    .filter(p -> p.id().equals(claims.authUserId()))
                    .findFirst()
                    .orElseThrow(() -> new OperacaoInvalidaException(
                            "Seu cadastro não consta como morador ativo da unidade."));
            reserva.setTipoReserva(TipoReserva.MORADOR);
            reserva.setUnidadeId(unidade);
            reserva.setResponsavelId(eu.id());
            reserva.setResponsavelNome(eu.nome());
            reserva.setResponsavelPerfil(eu.perfil());
            return;
        }

        if (tipo == TipoReserva.EVENTO_CONDOMINIO) {
            if (request.descricaoEvento() == null || request.descricaoEvento().isBlank()) {
                throw new OperacaoInvalidaException("Informe a descrição do evento.");
            }
            AuthApiClient.PessoaUnidade func = exigirResponsavel(
                    authApiClient.funcionarios(), request.responsavelId(),
                    "Selecione o funcionário responsável pelo evento.",
                    "O responsável informado não é um funcionário elegível.");
            reserva.setTipoReserva(TipoReserva.EVENTO_CONDOMINIO);
            reserva.setUnidadeId(null); // evento não tem unidade
            reserva.setResponsavelId(func.id());
            reserva.setResponsavelNome(func.nome());
            reserva.setResponsavelPerfil(func.perfil());
            reserva.setDescricaoEvento(request.descricaoEvento().trim());
            return;
        }

        if (request.unidadeId() == null || request.unidadeId().isBlank()) {
            throw new OperacaoInvalidaException("Selecione a unidade responsável pela reserva.");
        }
        AuthApiClient.PessoaUnidade morador = exigirResponsavel(
                authApiClient.pessoasDaUnidade(request.unidadeId()), request.responsavelId(),
                "Selecione o morador responsável pela reserva.",
                "O responsável informado não é morador ativo da unidade selecionada.");
        reserva.setTipoReserva(TipoReserva.MORADOR);
        reserva.setUnidadeId(request.unidadeId());
        reserva.setResponsavelId(morador.id());
        reserva.setResponsavelNome(morador.nome());
        reserva.setResponsavelPerfil(morador.perfil());
    }

    private AuthApiClient.PessoaUnidade exigirResponsavel(List<AuthApiClient.PessoaUnidade> candidatos,
                                                          String responsavelId,
                                                          String msgAusente, String msgInvalido) {
        if (responsavelId == null || responsavelId.isBlank()) {
            throw new OperacaoInvalidaException(msgAusente);
        }
        return candidatos.stream()
                .filter(p -> responsavelId.equals(p.id()))
                .findFirst()
                .orElseThrow(() -> new OperacaoInvalidaException(msgInvalido));
    }

    private boolean ehOperador(String perfil) {
        return "PORTEIRO".equals(perfil) || "ADMIN_SINDICO".equals(perfil) || "ADMIN_GERAL".equals(perfil);
    }

    // ─── Agenda ──────────────────────────────────────────────────────────────

    /** Reservas que tocam o período, incluindo as passadas (RN-03). */
    public List<Reserva> agenda(String areaComumId, LocalDateTime inicio, LocalDateTime fim) {
        AreaComum area = areaComunService.buscarPorId(areaComumId);
        exigirCondominioDaArea(area);
        // Uma pendente vencida não pode continuar aparecendo como ocupação.
        expirarPendentesVencidas();
        return reservaRepository.findNoPeriodo(areaComumId, inicio, fim);
    }

    // ─── Transições ──────────────────────────────────────────────────────────

    /**
     * Pendentes cujo prazo passou viram EXPIRADA, liberando o horário.
     * Chamado antes de checar conflito e antes de listar, em vez de por um job:
     * o efeito é o mesmo e não depende de agendador rodando.
     */
    public int expirarPendentesVencidas() {
        LocalDateTime agora = LocalDateTime.now();
        List<Reserva> vencidas =
                reservaRepository.findPendentesVencidas(agora.minus(PRAZO_APROVACAO), agora);
        for (Reserva r : vencidas) {
            StatusReserva anterior = r.getStatus();
            r.setStatus(StatusReserva.EXPIRADA);
            r.setAtualizadoEm(agora);
            reservaRepository.save(r);
            auditar(r, anterior, StatusReserva.EXPIRADA,
                    r.getInicio().isAfter(agora)
                            ? "Prazo de aprovação encerrado"
                            : "Horário reservado começou sem decisão",
                    null);
        }
        return vencidas.size();
    }

    /**
     * Momento em que a pendente expira: o que vier primeiro entre o fim do
     * prazo de resposta e o início do horário reservado.
     */
    public static LocalDateTime prazoDecisao(Reserva reserva) {
        LocalDateTime porTempoDeResposta = reserva.getCriadoEm().plus(PRAZO_APROVACAO);
        return porTempoDeResposta.isBefore(reserva.getInicio())
                ? porTempoDeResposta
                : reserva.getInicio();
    }

    public Reserva aprovar(String reservaId, JwtClaims claims) {
        exigirPermissaoAprovar(claims);
        Reserva reserva = buscarPorId(reservaId);
        exigirCondominio(reserva.getCondominioId());
        exigirTransicao(reserva, StatusReserva.APROVADA);

        LocalDateTime agora = LocalDateTime.now();
        if (reserva.getInicio().isBefore(agora)) {
            throw new OperacaoInvalidaException(
                    "Esta reserva já começou e não pode mais ser aprovada.");
        }

        AreaComum area = reserva.getAreaComum();
        validarJanelaFuncionamento(area, reserva.getInicio(), reserva.getFim());

        // RN-06: revalida o conflito no momento da aprovação. Entre a
        // solicitação e agora outra reserva pode ter sido aprovada.
        List<Reserva> conflitos = reservaRepository.findConflitantes(
                area.getId(), reserva.getInicio(), reserva.getFim(), reserva.getId());
        Reserva bloqueio = conflitos.stream()
                .filter(c -> c.getStatus() == StatusReserva.APROVADA
                        || c.getStatus() == StatusReserva.CONCLUIDA)
                .findFirst().orElse(null);
        if (bloqueio != null) {
            throw new OperacaoInvalidaException(
                    "Não é possível aprovar: o espaço já está reservado de "
                    + bloqueio.getInicio().format(DT_BR) + " até " + bloqueio.getFim().format(DT_BR)
                    + " por " + (bloqueio.getResponsavelNome() != null
                            ? bloqueio.getResponsavelNome() : "outro responsável") + ".");
        }

        StatusReserva anterior = reserva.getStatus();
        reserva.setStatus(StatusReserva.APROVADA);
        reserva.setAprovadoPorId(claims.authUserId());
        reserva.setAprovadoPorNome(claims.exibicao());
        reserva.setAprovadoEm(agora);
        reserva.setAtualizadoEm(agora);
        Reserva salva = reservaRepository.save(reserva);
        auditar(salva, anterior, StatusReserva.APROVADA, null, claims);
        return salva;
    }

    public Reserva recusar(String reservaId, String motivo, JwtClaims claims) {
        exigirPermissaoAprovar(claims);
        if (motivo == null || motivo.isBlank()) {
            throw new OperacaoInvalidaException(
                    "Informe o motivo da recusa — ele fica visível para quem solicitou.");
        }
        Reserva reserva = buscarPorId(reservaId);
        exigirCondominio(reserva.getCondominioId());
        exigirTransicao(reserva, StatusReserva.RECUSADA);

        StatusReserva anterior = reserva.getStatus();
        reserva.setStatus(StatusReserva.RECUSADA);
        reserva.setMotivoRecusa(motivo.trim());
        // Recusa também encerra a reserva: o histórico registra quem foi.
        reserva.setCanceladoPorId(claims.authUserId());
        reserva.setCanceladoPorNome(claims.exibicao());
        reserva.setCanceladoEm(LocalDateTime.now());
        reserva.setAtualizadoEm(LocalDateTime.now());
        Reserva salva = reservaRepository.save(reserva);
        auditar(salva, anterior, StatusReserva.RECUSADA, motivo.trim(), claims);
        return salva;
    }

    /** Aprova várias: as que conflitam ficam de fora, com o motivo (RN-06). */
    public ResultadoLote aprovarEmLote(List<String> ids, JwtClaims claims) {
        exigirPermissaoAprovar(claims);
        List<String> aprovadas = new java.util.ArrayList<>();
        List<FalhaLote> falhas = new java.util.ArrayList<>();
        for (String id : ids) {
            try {
                aprovar(id, claims);
                aprovadas.add(id);
            } catch (RuntimeException e) {
                falhas.add(new FalhaLote(id, e.getMessage()));
            }
        }
        return new ResultadoLote(aprovadas, falhas);
    }

    public record FalhaLote(String reservaId, String motivo) {}
    public record ResultadoLote(List<String> aprovadas, List<FalhaLote> falhas) {}

    @Transactional(readOnly = true)
    public List<portaria.model.ReservaAuditoria> auditoriaDe(String reservaId) {
        return auditoriaRepository.findByReservaIdOrderByCriadoEmDesc(reservaId);
    }

    /** Fila do síndico: pendentes ordenadas pela proximidade do prazo. */
    public List<Reserva> listarPendentes() {
        expirarPendentesVencidas();
        return reservaRepository.findPendentes(CondominioUtils.condominioIdEfetivo());
    }

    private void exigirPermissaoAprovar(JwtClaims claims) {
        if (claims == null || !podeAprovar(claims.perfil())) {
            throw new AcessoNegadoException("Seu perfil não pode aprovar ou recusar reservas.");
        }
    }

    private void exigirTransicao(Reserva r, StatusReserva novo) {
        if (!r.getStatus().podeIrPara(novo)) {
            throw new OperacaoInvalidaException(
                    "Não é possível passar de " + r.getStatus() + " para " + novo + ".");
        }
    }

    private void auditar(Reserva r, StatusReserva anterior, StatusReserva novo,
                         String motivo, JwtClaims claims) {
        var a = new portaria.model.ReservaAuditoria();
        a.setReservaId(r.getId());
        a.setCondominioId(r.getCondominioId());
        a.setStatusAnterior(anterior);
        a.setStatusNovo(novo);
        a.setMotivo(motivo);
        a.setAutorId(claims != null ? claims.authUserId() : null);
        a.setAutorNome(claims != null ? claims.exibicao() : "sistema");
        auditoriaRepository.save(a);
    }

    /** RN-07: o morador cancela apenas as próprias reservas. */
    public Reserva cancelar(String reservaId, JwtClaims claims) {
        Reserva reserva = buscarPorId(reservaId);
        exigirCondominio(reserva.getCondominioId());

        if (!ehOperador(claims.perfil())) {
            boolean minha = claims.authUserId().equals(reserva.getSolicitanteId())
                    || claims.authUserId().equals(reserva.getResponsavelId());
            if (!minha) {
                throw new AcessoNegadoException("Você só pode cancelar as suas próprias reservas.");
            }
        }
        exigirTransicao(reserva, StatusReserva.CANCELADA);

        StatusReserva anterior = reserva.getStatus();
        reserva.setStatus(StatusReserva.CANCELADA);
        reserva.setCanceladoPorId(claims.authUserId());
        reserva.setCanceladoPorNome(claims.exibicao());
        reserva.setCanceladoEm(LocalDateTime.now());
        reserva.setAtualizadoEm(LocalDateTime.now());
        Reserva salva = reservaRepository.save(reserva);
        auditar(salva, anterior, StatusReserva.CANCELADA, null, claims);
        return salva;
    }

    // ─── Consultas ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Reserva buscarPorId(String id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada com id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarPorCondominio(String condominioId) {
        if (condominioId == null) return reservaRepository.findAll();
        return reservaRepository.findByCondominioId(condominioId);
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarPorCondominioEStatus(String condominioId, StatusReserva status) {
        if (condominioId == null) return reservaRepository.findAll()
                .stream().filter(r -> r.getStatus() == status).toList();
        return reservaRepository.findByCondominioIdAndStatus(condominioId, status);
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarDoSolicitante(String solicitanteId) {
        return reservaRepository.findBySolicitanteId(solicitanteId);
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarPorArea(String areaComumId) {
        return reservaRepository.findByAreaComum_Id(areaComumId);
    }

    // ─── Escopo (RN-07) ──────────────────────────────────────────────────────

    private void exigirCondominioDaArea(AreaComum area) {
        exigirCondominio(area.getCondominioId());
    }

    private void exigirCondominio(String condominioIdDoRecurso) {
        String meu = CondominioUtils.condominioIdEfetivo();
        if (meu == null) return; // ADMIN_GERAL: sem filtro
        if (condominioIdDoRecurso != null && !meu.equals(condominioIdDoRecurso)) {
            throw new AcessoNegadoException("Este recurso não pertence ao seu condomínio.");
        }
    }
}
