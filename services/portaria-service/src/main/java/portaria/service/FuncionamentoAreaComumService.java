package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portaria.dto.reserva.FuncionamentoDTO;
import portaria.exception.OperacaoInvalidaException;
import portaria.model.AreaComum;
import portaria.model.AreaComumHorario;
import portaria.model.Reserva;
import portaria.model.enums.ModoFuncionamento;
import portaria.repository.AreaComumHorarioRepository;
import portaria.repository.AreaComunRepository;
import portaria.repository.ReservaRepository;
import portaria.util.FuncionamentoUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FuncionamentoAreaComumService {

    private static final DateTimeFormatter DT_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AreaComunRepository areaComunRepository;
    private final AreaComumHorarioRepository horarioRepository;
    private final ReservaRepository reservaRepository;

    @Transactional(readOnly = true)
    public List<AreaComumHorario> horariosDe(String areaComumId) {
        return horarioRepository.findByAreaComumId(areaComumId);
    }

    @Transactional(readOnly = true)
    public FuncionamentoDTO consultar(String areaComumId) {
        AreaComum area = buscarArea(areaComumId);
        List<FuncionamentoDTO.HorarioDia> dias = horariosDe(areaComumId).stream()
                .map(h -> new FuncionamentoDTO.HorarioDia(
                        h.getDiaSemana(), h.isFechado(), h.getAbertura(), h.getFechamento()))
                .sorted((a, b) -> a.diaSemana().compareTo(b.diaSemana()))
                .toList();
        return new FuncionamentoDTO(area.getModoFuncionamento(), area.isFuncionamentoARevisar(), dias);
    }

    public FuncionamentoDTO salvar(String areaComumId, FuncionamentoDTO dto) {
        AreaComum area = buscarArea(areaComumId);
        if (dto.modo() == null) {
            throw new OperacaoInvalidaException("Informe o modo de funcionamento do espaço.");
        }

        List<AreaComumHorario> novos = dto.modo() == ModoFuncionamento.HORARIOS_DEFINIDOS
                ? validarEMontar(areaComumId, dto)
                : List.of();

        // RN-09: alteração de configuração não pode invalidar reserva já marcada.
        bloquearSeConflitaComFuturas(area, dto.modo(), novos);

        horarioRepository.deleteByAreaComumId(areaComumId);
        // Sem o flush, o JPA emite os INSERTs antes do DELETE na mesma transação
        // e as linhas novas colidem com as antigas na constraint (area, dia).
        horarioRepository.flush();
        if (!novos.isEmpty()) horarioRepository.saveAll(novos);

        area.setModoFuncionamento(dto.modo());
        area.setFuncionamentoARevisar(false); // configurado à mão deixa de ser pendência
        area.setAtualizadoEm(LocalDateTime.now());
        areaComunRepository.save(area);

        return consultar(areaComumId);
    }

    private List<AreaComumHorario> validarEMontar(String areaComumId, FuncionamentoDTO dto) {
        if (dto.horarios() == null || dto.horarios().isEmpty()) {
            throw new OperacaoInvalidaException("Configure os horários dos dias da semana.");
        }

        List<AreaComumHorario> montados = new ArrayList<>();
        for (FuncionamentoDTO.HorarioDia d : dto.horarios()) {
            if (d.diaSemana() == null) {
                throw new OperacaoInvalidaException("Dia da semana inválido na configuração.");
            }
            AreaComumHorario h = new AreaComumHorario();
            h.setAreaComumId(areaComumId);
            h.setDiaSemana(d.diaSemana());

            if (d.fechado()) {
                h.setFechado(true);
                montados.add(h);
                continue;
            }
            if (d.abertura() == null || d.fechamento() == null) {
                throw new OperacaoInvalidaException(
                        "Informe abertura e fechamento de " + FuncionamentoUtils.nomeDia(d.diaSemana())
                        + ", ou marque o dia como fechado.");
            }
            // Igual é ambíguo entre "zero horas" e "24 horas"; para 24h existe o
            // modo sem restrição.
            if (d.abertura().equals(d.fechamento())) {
                throw new OperacaoInvalidaException(
                        "Em " + FuncionamentoUtils.nomeDia(d.diaSemana())
                        + ", abertura e fechamento não podem ser iguais. "
                        + "Para 24 horas, use \"sem restrição de horário\".");
            }
            h.setFechado(false);
            h.setAbertura(d.abertura());
            h.setFechamento(d.fechamento());
            montados.add(h);
        }

        boolean algumAberto = montados.stream().anyMatch(h -> !h.isFechado());
        if (!algumAberto) {
            throw new OperacaoInvalidaException(
                    "Ao menos um dia precisa estar aberto. Com todos fechados o espaço fica não reservável.");
        }
        return montados;
    }

    /**
     * Ampliar janelas ou mudar para sem restrição é sempre permitido — o que
     * bloqueia é reserva futura que deixaria de caber na nova configuração.
     */
    private void bloquearSeConflitaComFuturas(AreaComum area, ModoFuncionamento modo,
                                              List<AreaComumHorario> novos) {
        if (modo == ModoFuncionamento.SEM_RESTRICAO) return;

        List<Reserva> futuras = reservaRepository.findFuturas(area.getId(), LocalDateTime.now());
        List<Reserva> conflitantes = futuras.stream()
                // Mesma regra do cadastro: o que foi aceito pelas pontas não pode
                // ser considerado "fora do horário" por um critério mais estrito.
                .filter(r -> !FuncionamentoUtils.permitidoInicioEFim(novos, r.getInicio(), r.getFim()))
                .toList();

        if (conflitantes.isEmpty()) return;

        String detalhe = conflitantes.stream()
                .map(r -> "• " + r.getInicio().format(DT_BR) + " às " + r.getFim().format(DT_BR)
                        + " — " + (r.getResponsavelNome() != null ? r.getResponsavelNome() : "responsável não informado"))
                .collect(Collectors.joining("\n"));

        throw new OperacaoInvalidaException(
                "Não é possível alterar o funcionamento: existem " + conflitantes.size()
                + " reservas futuras fora do novo horário.\n" + detalhe);
    }

    private AreaComum buscarArea(String areaComumId) {
        return areaComunRepository.findById(areaComumId)
                .orElseThrow(() -> new portaria.exception.RecursoNaoEncontradoException(
                        "Área comum não encontrada: " + areaComumId));
    }
}
