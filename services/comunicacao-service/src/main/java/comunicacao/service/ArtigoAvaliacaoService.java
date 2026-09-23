package comunicacao.service;

import comunicacao.dto.FaqDTOs.AvaliacaoResumo;
import comunicacao.exception.OperacaoInvalidaException;
import comunicacao.exception.RecursoNaoEncontradoException;
import comunicacao.model.ArtigoAvaliacao;
import comunicacao.model.ArtigoConhecimento;
import comunicacao.repository.ArtigoAvaliacaoRepository;
import comunicacao.repository.ArtigoConhecimentoRepository;
import comunicacao.security.CondominioUtils;
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
public class ArtigoAvaliacaoService {

    private final ArtigoAvaliacaoRepository avaliacaoRepository;
    private final ArtigoConhecimentoRepository artigoRepository;

    /** Registra ou troca o voto do usuário. Votar de novo no mesmo sentido não duplica. */
    public AvaliacaoResumo avaliar(UUID artigoId, String usuarioId, boolean util) {
        ArtigoConhecimento artigo = artigoVisivel(artigoId);
        if (!artigo.isPublicado()) {
            throw new OperacaoInvalidaException("Só é possível avaliar artigos publicados");
        }
        ArtigoAvaliacao avaliacao = avaliacaoRepository.findByArtigoIdAndUsuarioId(artigoId, usuarioId)
                .orElseGet(() -> {
                    ArtigoAvaliacao nova = new ArtigoAvaliacao();
                    nova.setArtigoId(artigoId);
                    nova.setUsuarioId(usuarioId);
                    return nova;
                });
        avaliacao.setUtil(util);
        avaliacao.setAtualizadoEm(LocalDateTime.now());
        avaliacaoRepository.save(avaliacao);
        return resumo(artigoId, util);
    }

    public AvaliacaoResumo removerAvaliacao(UUID artigoId, String usuarioId) {
        artigoVisivel(artigoId);
        avaliacaoRepository.findByArtigoIdAndUsuarioId(artigoId, usuarioId)
                .ifPresent(avaliacaoRepository::delete);
        return resumo(artigoId, null);
    }

    /**
     * Totais de todos os artigos do condomínio, com o voto de quem pediu.
     *
     * Uma consulta agrupada para a lista inteira: N artigos não podem virar N
     * idas ao banco.
     */
    @Transactional(readOnly = true)
    public List<AvaliacaoResumo> resumoDoCondominio(String usuarioId) {
        String condominioId = CondominioUtils.condominioIdEfetivo();
        List<UUID> ids = (condominioId != null
                ? artigoRepository.findByCondominioId(condominioId)
                : artigoRepository.findAll())
                .stream().map(ArtigoConhecimento::getId).toList();
        if (ids.isEmpty()) return List.of();

        Map<UUID, long[]> totais = new HashMap<>();
        for (Object[] linha : avaliacaoRepository.contarPorArtigo(ids)) {
            totais.put((UUID) linha[0], new long[]{((Number) linha[1]).longValue(), ((Number) linha[2]).longValue()});
        }
        Map<UUID, Boolean> meus = new HashMap<>();
        for (ArtigoAvaliacao a : avaliacaoRepository.findByUsuarioIdAndArtigoIdIn(usuarioId, ids)) {
            meus.put(a.getArtigoId(), a.isUtil());
        }
        return ids.stream()
                .filter(id -> totais.containsKey(id) || meus.containsKey(id))
                .map(id -> {
                    long[] t = totais.getOrDefault(id, new long[]{0, 0});
                    return new AvaliacaoResumo(id, t[0], t[1], meus.get(id));
                })
                .toList();
    }

    private AvaliacaoResumo resumo(UUID artigoId, Boolean meuVoto) {
        return new AvaliacaoResumo(
                artigoId,
                avaliacaoRepository.countByArtigoIdAndUtil(artigoId, true),
                avaliacaoRepository.countByArtigoIdAndUtil(artigoId, false),
                meuVoto
        );
    }

    /** O artigo existe e pertence ao condomínio de quem pede (ou é global). */
    private ArtigoConhecimento artigoVisivel(UUID artigoId) {
        ArtigoConhecimento artigo = artigoRepository.findById(artigoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Artigo não encontrado"));
        String condominioId = CondominioUtils.condominioIdEfetivo();
        if (condominioId != null && artigo.getCondominioId() != null
                && !condominioId.equals(artigo.getCondominioId())) {
            throw new RecursoNaoEncontradoException("Artigo não encontrado");
        }
        return artigo;
    }
}
