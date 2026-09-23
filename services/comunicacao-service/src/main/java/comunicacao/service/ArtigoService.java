package comunicacao.service;

import comunicacao.exception.RecursoNaoEncontradoException;
import comunicacao.model.ArtigoConhecimento;
import comunicacao.model.enums.CategoriaArtigo;
import comunicacao.repository.ArtigoConhecimentoRepository;
import comunicacao.security.AuthContext;
import comunicacao.security.CondominioUtils;
import comunicacao.security.PerfilUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtigoService {

    private final ArtigoConhecimentoRepository artigoRepository;

    public ArtigoConhecimento criar(ArtigoConhecimento artigo) {
        PerfilUtils.exigirGestor();
        var claims = AuthContext.get();
        artigo.setId(null);
        artigo.setCondominioId(CondominioUtils.condominioIdEfetivo());
        if (artigo.getAutor() == null && claims != null) {
            artigo.setAutor(claims.email());
        }
        artigo.setCriadoEm(LocalDateTime.now());
        artigo.setAtualizadoEm(LocalDateTime.now());
        return artigoRepository.save(artigo);
    }

    public ArtigoConhecimento publicar(UUID id) {
        PerfilUtils.exigirGestor();
        ArtigoConhecimento artigo = buscarPorId(id);
        artigo.setPublicado(true);
        artigo.setAtualizadoEm(LocalDateTime.now());
        return artigoRepository.save(artigo);
    }

    public ArtigoConhecimento despublicar(UUID id) {
        PerfilUtils.exigirGestor();
        ArtigoConhecimento artigo = buscarPorId(id);
        artigo.setPublicado(false);
        artigo.setAtualizadoEm(LocalDateTime.now());
        return artigoRepository.save(artigo);
    }

    /**
     * Atualiza o artigo.
     *
     * O campo publicado entra na copia: sem ele o botao de publicar da tela de
     * gestao devolvia 200 sem ter mudado nada.
     */
    public ArtigoConhecimento atualizar(UUID id, ArtigoConhecimento dados) {
        PerfilUtils.exigirGestor();
        ArtigoConhecimento artigo = buscarPorId(id);
        artigo.setTitulo(dados.getTitulo());
        artigo.setConteudo(dados.getConteudo());
        artigo.setCategoria(dados.getCategoria());
        if (dados.getAutor() != null) artigo.setAutor(dados.getAutor());
        artigo.setPublicado(dados.isPublicado());
        artigo.setAtualizadoEm(LocalDateTime.now());
        return artigoRepository.save(artigo);
    }

    public void excluir(UUID id) {
        PerfilUtils.exigirGestor();
        if (!artigoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Artigo nao encontrado com id: " + id);
        }
        artigoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ArtigoConhecimento buscarPorId(UUID id) {
        return artigoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Artigo nao encontrado com id: " + id));
    }

    /**
     * Artigos publicados do condominio.
     *
     * O filtro por categoria e combinado com o condominio, e nao alternativo a
     * ele: filtrar so por categoria devolvia artigos de outros condominios.
     */
    @Transactional(readOnly = true)
    public List<ArtigoConhecimento> listarPublicados(String condominioId, CategoriaArtigo categoria) {
        if (condominioId != null && categoria != null) {
            return artigoRepository.findByCondominioIdAndCategoriaAndPublicadoTrue(condominioId, categoria);
        }
        if (condominioId != null) return artigoRepository.findByCondominioIdAndPublicadoTrue(condominioId);
        if (categoria != null) return artigoRepository.findByCategoriaAndPublicadoTrue(categoria);
        return artigoRepository.findByPublicadoTrue();
    }

    @Transactional(readOnly = true)
    public List<ArtigoConhecimento> listarTodos(String condominioId) {
        if (condominioId != null) return artigoRepository.findByCondominioId(condominioId);
        return artigoRepository.findAll();
    }

    /** Busca por titulo. Moradores so alcancam o que ja foi publicado. */
    @Transactional(readOnly = true)
    public List<ArtigoConhecimento> buscarPorTitulo(String titulo, String condominioId) {
        boolean apenasPublicados = !PerfilUtils.isAdministracao(PerfilUtils.perfilAtual());
        return artigoRepository.buscarPorTitulo(
                titulo == null ? "" : titulo.trim(), condominioId, apenasPublicados);
    }
}
