package comunicacao.service;

import comunicacao.model.AvisoLeitura;
import comunicacao.repository.AvisoLeituraRepository;
import comunicacao.repository.AvisoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AvisoLeituraService {

    private final AvisoLeituraRepository leituraRepository;
    private final AvisoRepository avisoRepository;

    public AvisoLeitura marcarLido(UUID avisoId, String usuarioId) {
        return leituraRepository.findByAvisoIdAndUsuarioId(avisoId, usuarioId)
                .orElseGet(() -> {
                    if (!avisoRepository.existsById(avisoId)) {
                        throw new comunicacao.exception.RecursoNaoEncontradoException(
                                "Aviso não encontrado com id: " + avisoId);
                    }
                    AvisoLeitura leitura = new AvisoLeitura();
                    leitura.setAvisoId(avisoId);
                    leitura.setUsuarioId(usuarioId);
                    return leituraRepository.save(leitura);
                });
    }

    @Transactional(readOnly = true)
    public boolean jaLeu(UUID avisoId, String usuarioId) {
        return leituraRepository.findByAvisoIdAndUsuarioId(avisoId, usuarioId).isPresent();
    }

    @Transactional(readOnly = true)
    public long contarLeituras(UUID avisoId) {
        return leituraRepository.countByAvisoId(avisoId);
    }

    /** Confirmações de cada aviso da lista, agrupadas no banco. */
    @Transactional(readOnly = true)
    public Map<UUID, Long> confirmacoesPorAviso(List<UUID> avisos) {
        Map<UUID, Long> porAviso = new HashMap<>();
        if (avisos.isEmpty()) return porAviso;
        for (Object[] linha : leituraRepository.contarPorAviso(avisos)) {
            porAviso.put((UUID) linha[0], (Long) linha[1]);
        }
        return porAviso;
    }

    /** Quem leu este aviso, e quando. */
    @Transactional(readOnly = true)
    public Map<String, LocalDateTime> leiturasDoAviso(UUID avisoId) {
        Map<String, LocalDateTime> porUsuario = new HashMap<>();
        for (AvisoLeitura leitura : leituraRepository.findByAvisoId(avisoId)) {
            porUsuario.put(leitura.getUsuarioId(), leitura.getLidoEm());
        }
        return porUsuario;
    }

    @Transactional(readOnly = true)
    public List<UUID> avisosLidosPeloUsuario(String usuarioId) {
        return leituraRepository.findAvisosLidosByUsuario(usuarioId);
    }

    /**
     * Quando este usuário leu cada aviso.
     *
     * Uma consulta para a lista inteira, e não uma por aviso: a tela mostra
     * todos os ativos de uma vez, e N avisos não podem virar N idas ao banco.
     */
    @Transactional(readOnly = true)
    public Map<UUID, LocalDateTime> quandoLeu(String usuarioId) {
        Map<UUID, LocalDateTime> porAviso = new HashMap<>();
        for (AvisoLeitura leitura : leituraRepository.findByUsuarioId(usuarioId)) {
            porAviso.put(leitura.getAvisoId(), leitura.getLidoEm());
        }
        return porAviso;
    }
}
