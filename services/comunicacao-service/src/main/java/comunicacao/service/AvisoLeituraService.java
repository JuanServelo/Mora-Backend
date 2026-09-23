package comunicacao.service;

import comunicacao.client.AuthApiClient;
import comunicacao.dto.LeituraAvisoResponse;
import comunicacao.dto.LeitorResponse;
import comunicacao.dto.UsuarioResumo;
import comunicacao.exception.RecursoNaoEncontradoException;
import comunicacao.model.Aviso;
import comunicacao.model.AvisoLeitura;
import comunicacao.repository.AvisoLeituraRepository;
import comunicacao.repository.AvisoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AvisoLeituraService {

    private final AvisoLeituraRepository leituraRepository;
    private final AvisoRepository avisoRepository;
    private final AuthApiClient authApi;

    /**
     * Registra a ciência do usuário. É idempotente: reabrir o aviso devolve a
     * leitura original, preservando a data em que a pessoa realmente leu.
     */
    public AvisoLeitura marcarLido(UUID avisoId, String usuarioId) {
        return leituraRepository.findByAvisoIdAndUsuarioId(avisoId, usuarioId)
                .orElseGet(() -> {
                    if (!avisoRepository.existsById(avisoId)) {
                        throw new RecursoNaoEncontradoException("Aviso não encontrado com id: " + avisoId);
                    }
                    AvisoLeitura leitura = new AvisoLeitura();
                    leitura.setAvisoId(avisoId);
                    leitura.setUsuarioId(usuarioId);
                    return leituraRepository.save(leitura);
                });
    }

    @Transactional(readOnly = true)
    public long contarLeituras(UUID avisoId) {
        return leituraRepository.countByAvisoId(avisoId);
    }

    /** Avisos que o usuário já leu, para marcar a lista de uma vez só. */
    @Transactional(readOnly = true)
    public Set<UUID> avisosLidosPor(String usuarioId) {
        return new HashSet<>(leituraRepository.findAvisosLidosByUsuario(usuarioId));
    }

    /** Contagem de leituras por aviso, em uma consulta. */
    @Transactional(readOnly = true)
    public Map<UUID, Long> contagemPorAviso(Collection<UUID> avisoIds) {
        if (avisoIds.isEmpty()) return Map.of();
        Map<UUID, Long> contagem = new HashMap<>();
        for (Object[] linha : leituraRepository.contarPorAviso(avisoIds)) {
            contagem.put((UUID) linha[0], (Long) linha[1]);
        }
        avisoIds.forEach(id -> contagem.putIfAbsent(id, 0L));
        return contagem;
    }

    /**
     * Quem leu, quem falta e o percentual.
     *
     * O denominador vem do auth-api, filtrado pelo público-alvo do aviso — é a
     * única fonte que sabe quantas pessoas deveriam ter recebido. Se ele não
     * responder, devolvemos as leituras sem percentual: um número parcial é
     * pior que nenhum quando serve de comprovação.
     */
    @Transactional(readOnly = true)
    public LeituraAvisoResponse estatisticas(UUID avisoId) {
        Aviso aviso = avisoRepository.findById(avisoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aviso não encontrado com id: " + avisoId));

        List<AvisoLeitura> leituras = leituraRepository.findByAvisoIdOrderByLidoEmDesc(avisoId);
        Map<String, UsuarioResumo> porId = authApi.buscarPorIds(
                leituras.stream().map(AvisoLeitura::getUsuarioId).toList());

        List<LeitorResponse> leitores = leituras.stream().map(l -> {
            UsuarioResumo u = porId.get(l.getUsuarioId());
            return new LeitorResponse(
                    l.getUsuarioId(),
                    u == null ? null : u.nome(),
                    u == null ? null : u.perfil(),
                    u == null ? null : u.unidade(),
                    l.getLidoEm());
        }).toList();

        String publicoAlvo = aviso.getPublicoAlvo() == null ? "TODOS" : aviso.getPublicoAlvo().name();
        List<UsuarioResumo> destinatarios = authApi.destinatarios(publicoAlvo);

        Set<String> jaLeram = leituras.stream().map(AvisoLeitura::getUsuarioId).collect(Collectors.toSet());
        List<UsuarioResumo> pendentes = destinatarios.stream()
                .filter(u -> !jaLeram.contains(u.id()))
                .toList();

        long total = destinatarios.size();
        Double percentual = total == 0 ? null
                : Math.round(leituras.size() * 10000.0 / total) / 100.0;

        return new LeituraAvisoResponse(
                avisoId.toString(), total, leituras.size(), percentual, leitores, pendentes);
    }
}
