package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portaria.dto.jornada.AvaliacaoEntradaDTO;
import portaria.dto.jornada.JornadaDTO;
import portaria.exception.AcessoNegadoException;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.FuncionarioAuditoria;
import portaria.model.FuncionarioJornada;
import portaria.model.LiberacaoExcepcional;
import portaria.model.TurnoDia;
import portaria.model.enums.SituacaoFuncional;
import portaria.model.enums.TipoJornada;
import portaria.repository.FuncionarioAuditoriaRepository;
import portaria.repository.FuncionarioJornadaRepository;
import portaria.repository.LiberacaoExcepcionalRepository;
import portaria.repository.TurnoDiaRepository;
import portaria.security.AuthContext;
import portaria.security.CondominioUtils;
import portaria.security.JwtClaims;
import portaria.util.JornadaUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class JornadaService {

    private final FuncionarioJornadaRepository jornadaRepository;
    private final TurnoDiaRepository turnoDiaRepository;
    private final LiberacaoExcepcionalRepository liberacaoRepository;
    private final FuncionarioAuditoriaRepository auditoriaRepository;

    // ─── Leitura ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<JornadaDTO> listar() {
        return jornadaRepository.findByCondominioId(CondominioUtils.condominioIdEfetivo())
                .stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public JornadaDTO consultar(String authUserId) {
        return toDTO(exigirJornada(authUserId));
    }

    @Transactional(readOnly = true)
    public List<JornadaUtils.ProximoPlantao> proximosPlantoes(String authUserId, int dias) {
        FuncionarioJornada j = exigirJornada(authUserId);
        return JornadaUtils.proximosPlantoes(j, turnoDiaRepository.findByJornadaId(j.getId()),
                LocalDate.now(), dias);
    }

    /** Prévia usada antes de confirmar mudança de âncora do ciclo (RN-03). */
    @Transactional(readOnly = true)
    public List<JornadaUtils.ProximoPlantao> previa(JornadaDTO dto, int dias) {
        FuncionarioJornada simulada = new FuncionarioJornada();
        simulada.setTipoJornada(dto.tipoJornada());
        simulada.setCicloInicio(dto.cicloInicio());
        simulada.setCicloTamanho(dto.cicloTamanho());
        return JornadaUtils.proximosPlantoes(simulada, montarDias(dto, "previa"),
                LocalDate.now(), dias);
    }

    // ─── Gravação ────────────────────────────────────────────────────────────

    public JornadaDTO salvar(JornadaDTO dto) {
        if (dto.authUserId() == null || dto.authUserId().isBlank()) {
            throw new OperacaoInvalidaException("Informe o funcionário.");
        }
        String condominioId = CondominioUtils.condominioIdEfetivo();
        JwtClaims claims = AuthContext.get();

        FuncionarioJornada jornada = jornadaRepository
                .findByAuthUserIdAndCondominioId(dto.authUserId(), condominioId)
                .orElseGet(FuncionarioJornada::new);

        boolean novo = jornada.getId() == null;
        String situacaoAnterior = novo ? null : String.valueOf(jornada.getSituacao());
        String jornadaAnterior = novo ? null : resumoJornada(jornada);

        validar(dto);

        jornada.setAuthUserId(dto.authUserId());
        jornada.setNome(dto.nome());
        jornada.setCondominioId(condominioId);
        jornada.setSituacao(dto.situacao() != null ? dto.situacao() : SituacaoFuncional.ATIVO);
        jornada.setTipoJornada(dto.tipoJornada());
        jornada.setCicloInicio(dto.tipoJornada() == TipoJornada.ESCALA ? dto.cicloInicio() : null);
        jornada.setCicloTamanho(dto.tipoJornada() == TipoJornada.ESCALA ? dto.cicloTamanho() : null);
        jornada.setObservacoes(dto.observacoes());
        jornada.setAtualizadoEm(LocalDateTime.now());
        FuncionarioJornada salva = jornadaRepository.save(jornada);

        // Flush entre apagar e regravar: sem isso o JPA emite os INSERTs antes
        // do DELETE e colide na unicidade, como já aconteceu nos horários de área.
        turnoDiaRepository.deleteByJornadaId(salva.getId());
        turnoDiaRepository.flush();
        List<TurnoDia> dias = montarDias(dto, salva.getId());
        if (!dias.isEmpty()) turnoDiaRepository.saveAll(dias);

        registrarAuditoria(salva, claims, "situacao", situacaoAnterior, String.valueOf(salva.getSituacao()));
        registrarAuditoria(salva, claims, "jornada", jornadaAnterior, resumoJornada(salva));

        return toDTO(salva);
    }

    private void validar(JornadaDTO dto) {
        if (dto.tipoJornada() == null) {
            throw new OperacaoInvalidaException("Informe o tipo de jornada.");
        }
        if (dto.dias() == null || dto.dias().isEmpty()) {
            throw new OperacaoInvalidaException("Configure os dias da jornada.");
        }
        if (dto.tipoJornada() == TipoJornada.ESCALA) {
            if (dto.cicloInicio() == null) {
                throw new OperacaoInvalidaException("Informe a data de início do ciclo.");
            }
            if (dto.cicloTamanho() == null || dto.cicloTamanho() < 2 || dto.cicloTamanho() > 31) {
                throw new OperacaoInvalidaException("O ciclo deve ter entre 2 e 31 dias.");
            }
        }
        boolean algumEmServico = false;
        for (JornadaDTO.DiaDTO d : dto.dias()) {
            if (d.folga()) continue;
            algumEmServico = true;
            if (d.inicio() == null || d.fim() == null) {
                throw new OperacaoInvalidaException(
                        "Informe início e fim dos dias em serviço, ou marque-os como folga.");
            }
            // Igual seria ambíguo entre zero e 24 horas.
            if (d.inicio().equals(d.fim())) {
                throw new OperacaoInvalidaException(
                        "Início e fim do turno não podem ser iguais.");
            }
        }
        if (!algumEmServico) {
            throw new OperacaoInvalidaException("Ao menos um dia precisa estar em serviço.");
        }
    }

    private List<TurnoDia> montarDias(JornadaDTO dto, String jornadaId) {
        List<TurnoDia> dias = new ArrayList<>();
        for (JornadaDTO.DiaDTO d : dto.dias()) {
            TurnoDia t = new TurnoDia();
            t.setJornadaId(jornadaId);
            t.setDiaSemana(dto.tipoJornada() == TipoJornada.SEMANAL_FIXA ? d.diaSemana() : null);
            t.setPosicao(dto.tipoJornada() == TipoJornada.ESCALA ? d.posicao() : null);
            t.setFolga(d.folga());
            t.setInicio(d.folga() ? null : d.inicio());
            t.setFim(d.folga() ? null : d.fim());
            dias.add(t);
        }
        return dias;
    }

    // ─── Avaliação de entrada (RN-04) ────────────────────────────────────────

    /**
     * Decide se o funcionário pode entrar agora.
     *
     * Ordem importa: situação funcional vem primeiro porque liberação
     * excepcional sobrepõe apenas a validação de turno, nunca a situação.
     *
     * Sem jornada cadastrada a entrada é liberada — bloquear quem o síndico
     * ainda não configurou transformaria uma pendência administrativa em
     * porta trancada.
     */
    @Transactional(readOnly = true)
    public AvaliacaoEntradaDTO avaliarEntrada(String authUserId, LocalDateTime instante) {
        Optional<FuncionarioJornada> opt = jornadaRepository.findByAuthUserId(authUserId);
        if (opt.isEmpty()) return AvaliacaoEntradaDTO.permitido(null);

        FuncionarioJornada j = opt.get();
        String nome = j.getNome() != null ? j.getNome() : "O funcionário";

        if (!j.getSituacao().permiteEntrada()) {
            return AvaliacaoEntradaDTO.bloqueado(j.getSituacao().motivoBloqueio(nome), null);
        }

        List<TurnoDia> dias = turnoDiaRepository.findByJornadaId(j.getId());
        String turnoHoje = JornadaUtils.descricaoDoDia(j, dias, instante.toLocalDate());

        // O snapshot registra o turno em que a pessoa está, não o do dia do
        // calendário: na madrugada do plantão noturno o dia corrente é folga,
        // e gravar "Folga" no acesso contaria a história errada.
        var vigente = JornadaUtils.turnoVigente(j, dias, instante);
        if (vigente.isPresent()) {
            return AvaliacaoEntradaDTO.permitido(vigente.get().descricao());
        }

        // Fora do turno: só uma liberação vigente salva.
        LiberacaoExcepcional lib = liberacaoVigente(authUserId, instante).orElse(null);
        if (lib != null) {
            return AvaliacaoEntradaDTO.porLiberacao(turnoHoje, lib.getMotivo(), lib.getAutorizadoPorNome());
        }

        if ("Folga".equals(turnoHoje)) {
            return AvaliacaoEntradaDTO.bloqueado(nome + " está de folga hoje.", turnoHoje);
        }
        return AvaliacaoEntradaDTO.bloqueado(
                nome + " está fora do turno. Turno de hoje: " + turnoHoje + ".", turnoHoje);
    }

    /** Considera a véspera: liberação que vira a meia-noite ainda vale. */
    private Optional<LiberacaoExcepcional> liberacaoVigente(String authUserId, LocalDateTime instante) {
        List<LocalDate> datas = List.of(instante.toLocalDate().minusDays(1), instante.toLocalDate());
        return liberacaoRepository.findByAuthUserIdAndDataIn(authUserId, datas).stream()
                .filter(l -> l.vigenteEm(instante))
                .findFirst();
    }

    /** Consome a liberação usada na entrada; não se reaproveita depois. */
    public void marcarLiberacaoUtilizada(String authUserId, LocalDateTime instante) {
        liberacaoVigente(authUserId, instante).ifPresent(l -> {
            l.setStatus(portaria.model.enums.StatusPreAutorizacao.UTILIZADA);
            l.setUtilizadaEm(LocalDateTime.now());
            liberacaoRepository.save(l);
        });
    }

    // ─── Auditoria (RN-08) ───────────────────────────────────────────────────

    private void registrarAuditoria(FuncionarioJornada j, JwtClaims claims,
                                    String campo, String anterior, String novo) {
        if (java.util.Objects.equals(anterior, novo)) return; // nada mudou
        FuncionarioAuditoria a = new FuncionarioAuditoria();
        a.setAuthUserId(j.getAuthUserId());
        a.setFuncionarioNome(j.getNome());
        a.setCondominioId(j.getCondominioId());
        a.setCampo(campo);
        a.setValorAnterior(anterior);
        a.setValorNovo(novo);
        a.setAutorId(claims != null ? claims.authUserId() : null);
        a.setAutorNome(claims != null ? claims.exibicao() : "sistema");
        auditoriaRepository.save(a);
    }

    @Transactional(readOnly = true)
    public List<FuncionarioAuditoria> auditoriaDe(String authUserId) {
        return auditoriaRepository.findByAuthUserIdOrderByCriadoEmDesc(authUserId);
    }

    private String resumoJornada(FuncionarioJornada j) {
        if (j.getTipoJornada() == TipoJornada.ESCALA) {
            return "ESCALA ciclo=" + j.getCicloTamanho() + " a partir de " + j.getCicloInicio();
        }
        return "SEMANAL_FIXA";
    }

    // ─── Apoio ───────────────────────────────────────────────────────────────

    private FuncionarioJornada exigirJornada(String authUserId) {
        FuncionarioJornada j = jornadaRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Jornada não cadastrada para este funcionário."));
        String meu = CondominioUtils.condominioIdEfetivo();
        if (meu != null && j.getCondominioId() != null && !meu.equals(j.getCondominioId())) {
            throw new AcessoNegadoException("Este funcionário não pertence ao seu condomínio.");
        }
        return j;
    }

    private JornadaDTO toDTO(FuncionarioJornada j) {
        List<JornadaDTO.DiaDTO> dias = turnoDiaRepository.findByJornadaId(j.getId()).stream()
                .map(d -> new JornadaDTO.DiaDTO(d.getDiaSemana(), d.getPosicao(),
                        d.isFolga(), d.getInicio(), d.getFim()))
                .toList();
        return new JornadaDTO(j.getId(), j.getAuthUserId(), j.getNome(), j.getSituacao(),
                j.getTipoJornada(), j.getCicloInicio(), j.getCicloTamanho(), j.getObservacoes(), dias);
    }
}
