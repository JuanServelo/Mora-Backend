package comunicacao.service;

import comunicacao.dto.FaqDTOs.PerguntaRequest;
import comunicacao.dto.FaqDTOs.RespostaRequest;
import comunicacao.exception.AcessoNegadoException;
import comunicacao.exception.OperacaoInvalidaException;
import comunicacao.exception.RecursoNaoEncontradoException;
import comunicacao.model.ArtigoConhecimento;
import comunicacao.model.PerguntaFaq;
import comunicacao.model.enums.CategoriaArtigo;
import comunicacao.model.enums.StatusPergunta;
import comunicacao.repository.ArtigoConhecimentoRepository;
import comunicacao.repository.PerguntaFaqRepository;
import comunicacao.security.CondominioUtils;
import comunicacao.security.UsuarioAtual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PerguntaFaqService {

    private final PerguntaFaqRepository perguntaRepository;
    private final ArtigoConhecimentoRepository artigoRepository;

    public PerguntaFaq perguntar(PerguntaRequest dados) {
        String condominioId = CondominioUtils.condominioIdEfetivo();
        if (condominioId == null) {
            throw new OperacaoInvalidaException("Seu usuário não está vinculado a um condomínio");
        }
        PerguntaFaq pergunta = new PerguntaFaq();
        pergunta.setCondominioId(condominioId);
        pergunta.setAutorId(UsuarioAtual.id());
        pergunta.setAutorNome(UsuarioAtual.nome());
        pergunta.setTexto(dados.texto().trim());
        pergunta.setCategoria(dados.categoria());
        return perguntaRepository.save(pergunta);
    }

    @Transactional(readOnly = true)
    public List<PerguntaFaq> minhas() {
        return perguntaRepository.findByAutorIdOrderByCriadoEmDesc(UsuarioAtual.id());
    }

    /** Fila da administração. Sem condomínio no token (admin geral), vê todos. */
    @Transactional(readOnly = true)
    public List<PerguntaFaq> doCondominio(StatusPergunta status) {
        UsuarioAtual.exigirGestao();
        String condominioId = CondominioUtils.condominioIdEfetivo();
        if (condominioId == null) {
            return status == null
                    ? perguntaRepository.findAllByOrderByCriadoEmDesc()
                    : perguntaRepository.findByStatusOrderByCriadoEmDesc(status);
        }
        return status == null
                ? perguntaRepository.findByCondominioIdOrderByCriadoEmDesc(condominioId)
                : perguntaRepository.findByCondominioIdAndStatusOrderByCriadoEmDesc(condominioId, status);
    }

    /**
     * Responde a pergunta e, se pedido, publica a resposta como artigo da FAQ.
     *
     * Publicar é o que faz a FAQ crescer a partir das dúvidas reais: a próxima
     * pessoa com a mesma dúvida encontra a resposta na busca, sem perguntar.
     */
    public PerguntaFaq responder(UUID id, RespostaRequest dados) {
        UsuarioAtual.exigirGestao();
        PerguntaFaq pergunta = doMeuCondominio(id);
        if (pergunta.getStatus() == StatusPergunta.RESPONDIDA) {
            throw new OperacaoInvalidaException("Esta pergunta já foi respondida");
        }

        pergunta.setResposta(dados.resposta().trim());
        pergunta.setRespondidaPor(UsuarioAtual.nome());
        pergunta.setRespondidaEm(LocalDateTime.now());
        pergunta.setStatus(StatusPergunta.RESPONDIDA);

        if (dados.publicar()) {
            ArtigoConhecimento artigo = new ArtigoConhecimento();
            artigo.setTitulo(tituloDoArtigo(dados.titulo(), pergunta.getTexto()));
            artigo.setConteudo(pergunta.getResposta());
            artigo.setCategoria(dados.categoria() != null ? dados.categoria()
                    : pergunta.getCategoria() != null ? pergunta.getCategoria()
                    : CategoriaArtigo.FAQ);
            artigo.setAutor(UsuarioAtual.nome());
            artigo.setCondominioId(pergunta.getCondominioId());
            artigo.setPublicado(true);
            pergunta.setArtigoId(artigoRepository.save(artigo).getId());
        }
        return perguntaRepository.save(pergunta);
    }

    /** Quem perguntou pode desistir enquanto ninguém respondeu; a gestão pode descartar. */
    public void excluir(UUID id) {
        PerguntaFaq pergunta = perguntaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pergunta não encontrada"));
        boolean ehAutor = pergunta.getAutorId().equals(UsuarioAtual.id());
        if (UsuarioAtual.ehGestao()) {
            doMeuCondominio(id);
        } else if (!ehAutor) {
            throw new AcessoNegadoException("Você só pode excluir as suas próprias perguntas");
        } else if (pergunta.getStatus() == StatusPergunta.RESPONDIDA) {
            throw new OperacaoInvalidaException("A pergunta já foi respondida e não pode mais ser excluída");
        }
        perguntaRepository.delete(pergunta);
    }

    private PerguntaFaq doMeuCondominio(UUID id) {
        PerguntaFaq pergunta = perguntaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pergunta não encontrada"));
        String condominioId = CondominioUtils.condominioIdEfetivo();
        if (condominioId != null && !condominioId.equals(pergunta.getCondominioId())) {
            throw new RecursoNaoEncontradoException("Pergunta não encontrada");
        }
        return pergunta;
    }

    private static String tituloDoArtigo(String informado, String pergunta) {
        String base = informado != null && !informado.isBlank() ? informado.trim() : pergunta.trim();
        return base.length() > 200 ? base.substring(0, 197) + "..." : base;
    }
}
