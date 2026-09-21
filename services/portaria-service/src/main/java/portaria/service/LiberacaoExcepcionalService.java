package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portaria.exception.AcessoNegadoException;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.FuncionarioAuditoria;
import portaria.model.FuncionarioJornada;
import portaria.model.LiberacaoExcepcional;
import portaria.model.enums.StatusPreAutorizacao;
import portaria.repository.FuncionarioAuditoriaRepository;
import portaria.repository.FuncionarioJornadaRepository;
import portaria.repository.LiberacaoExcepcionalRepository;
import portaria.security.AuthContext;
import portaria.security.CondominioUtils;
import portaria.security.JwtClaims;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LiberacaoExcepcionalService {

    private final LiberacaoExcepcionalRepository repository;
    private final FuncionarioJornadaRepository jornadaRepository;
    private final FuncionarioAuditoriaRepository auditoriaRepository;

    public LiberacaoExcepcional criar(LiberacaoExcepcional pedido) {
        if (pedido.getAuthUserId() == null || pedido.getAuthUserId().isBlank()) {
            throw new OperacaoInvalidaException("Selecione o funcionário.");
        }
        if (pedido.getData() == null || pedido.getHoraInicio() == null || pedido.getHoraFim() == null) {
            throw new OperacaoInvalidaException("Informe a data e a janela de horário.");
        }
        if (pedido.getMotivo() == null || pedido.getMotivo().isBlank()) {
            throw new OperacaoInvalidaException("Informe o motivo da liberação.");
        }
        if (pedido.getHoraInicio().equals(pedido.getHoraFim())) {
            throw new OperacaoInvalidaException("O início e o fim da janela não podem ser iguais.");
        }

        FuncionarioJornada jornada = jornadaRepository.findByAuthUserId(pedido.getAuthUserId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Funcionário sem jornada cadastrada."));

        // Liberação sobrepõe turno, nunca situação funcional (RN-05).
        if (!jornada.getSituacao().permiteEntrada()) {
            throw new OperacaoInvalidaException(
                    "Não é possível liberar: " + jornada.getSituacao().motivoBloqueio(
                            jornada.getNome() != null ? jornada.getNome() : "o funcionário"));
        }

        LiberacaoExcepcional nova = new LiberacaoExcepcional();
        nova.setAuthUserId(pedido.getAuthUserId());
        nova.setFuncionarioNome(jornada.getNome());
        nova.setCondominioId(CondominioUtils.condominioIdEfetivo());
        nova.setData(pedido.getData());
        nova.setHoraInicio(pedido.getHoraInicio());
        nova.setHoraFim(pedido.getHoraFim());
        nova.setMotivo(pedido.getMotivo().trim());
        nova.setObservacoes(pedido.getObservacoes());
        nova.setStatus(StatusPreAutorizacao.AGUARDANDO);

        JwtClaims claims = AuthContext.get();
        nova.setAutorizadoPorId(claims != null ? claims.authUserId() : null);
        nova.setAutorizadoPorNome(claims != null ? claims.exibicao() : "sistema");
        nova.setAutorizadoEm(LocalDateTime.now());

        if (nova.fimEm().isBefore(LocalDateTime.now())) {
            throw new OperacaoInvalidaException("A janela informada já passou.");
        }

        LiberacaoExcepcional salva = repository.save(nova);
        auditar(salva, "liberacao_excepcional", null,
                salva.getData() + " " + salva.getHoraInicio() + "–" + salva.getHoraFim()
                        + " · " + salva.getMotivo(), claims);
        return salva;
    }

    public void cancelar(String id) {
        LiberacaoExcepcional l = buscarPorId(id);
        exigirMesmoCondominio(l);
        if (l.statusEfetivo() == StatusPreAutorizacao.UTILIZADA) {
            throw new OperacaoInvalidaException("Esta liberação já foi utilizada.");
        }
        if (l.statusEfetivo() == StatusPreAutorizacao.CANCELADA) {
            throw new OperacaoInvalidaException("Esta liberação já está cancelada.");
        }
        String anterior = String.valueOf(l.getStatus());
        l.setStatus(StatusPreAutorizacao.CANCELADA);
        repository.save(l);
        auditar(l, "liberacao_excepcional", anterior, "CANCELADA", AuthContext.get());
    }

    @Transactional(readOnly = true)
    public List<LiberacaoExcepcional> listar() {
        return repository.findByCondominioId(CondominioUtils.condominioIdEfetivo()).stream()
                .sorted(Comparator
                        .comparing((LiberacaoExcepcional l) -> l.statusEfetivo() != StatusPreAutorizacao.AGUARDANDO)
                        .thenComparing(LiberacaoExcepcional::getData, Comparator.reverseOrder()))
                .toList();
    }

    @Transactional(readOnly = true)
    public LiberacaoExcepcional buscarPorId(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Liberação não encontrada: " + id));
    }

    private void exigirMesmoCondominio(LiberacaoExcepcional l) {
        String meu = CondominioUtils.condominioIdEfetivo();
        if (meu != null && l.getCondominioId() != null && !meu.equals(l.getCondominioId())) {
            throw new AcessoNegadoException("Esta liberação não pertence ao seu condomínio.");
        }
    }

    private void auditar(LiberacaoExcepcional l, String campo, String anterior,
                         String novo, JwtClaims claims) {
        FuncionarioAuditoria a = new FuncionarioAuditoria();
        a.setAuthUserId(l.getAuthUserId());
        a.setFuncionarioNome(l.getFuncionarioNome());
        a.setCondominioId(l.getCondominioId());
        a.setCampo(campo);
        a.setValorAnterior(anterior);
        a.setValorNovo(novo);
        a.setAutorId(claims != null ? claims.authUserId() : null);
        a.setAutorNome(claims != null ? claims.exibicao() : "sistema");
        auditoriaRepository.save(a);
    }
}
