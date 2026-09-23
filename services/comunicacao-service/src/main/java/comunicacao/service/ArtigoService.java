package comunicacao.service;

import comunicacao.exception.RecursoNaoEncontradoException;
import comunicacao.model.ArtigoConhecimento;
import comunicacao.model.enums.CategoriaArtigo;
import comunicacao.repository.ArtigoConhecimentoRepository;
import comunicacao.security.AuthContext;
import comunicacao.security.Autorizacao;
import comunicacao.security.CondominioUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * A base de conhecimento e o FAQ.
 *
 * Mesmas regras do `AvisoService`, e pelo mesmo motivo: escrever é da
 * administração, ler é de quem usa o sistema, e nenhuma das duas atravessa a
 * fronteira do condomínio.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ArtigoService {

    private final ArtigoConhecimentoRepository artigoRepository;

    public ArtigoConhecimento criar(ArtigoConhecimento artigo) {
        Autorizacao.exigirGestaoDoCondominio("publicar artigos");

        var claims = AuthContext.get();
        artigo.setCondominioId(CondominioUtils.condominioIdEfetivo());
        if (artigo.getAutor() == null && claims != null) {
            artigo.setAutor(claims.email());
        }
        return artigoRepository.save(artigo);
    }

    public ArtigoConhecimento publicar(UUID id) {
        ArtigoConhecimento artigo = buscarParaEscrita(id, "publicar artigos");
        artigo.setPublicado(true);
        artigo.setAtualizadoEm(LocalDateTime.now());
        return artigoRepository.save(artigo);
    }

    public ArtigoConhecimento atualizar(UUID id, ArtigoConhecimento dados) {
        ArtigoConhecimento artigo = buscarParaEscrita(id, "editar artigos");
        artigo.setTitulo(dados.getTitulo());
        artigo.setConteudo(dados.getConteudo());
        artigo.setCategoria(dados.getCategoria());
        artigo.setAtualizadoEm(LocalDateTime.now());
        return artigoRepository.save(artigo);
    }

    public void excluir(UUID id) {
        ArtigoConhecimento artigo = buscarParaEscrita(id, "excluir artigos");
        artigoRepository.delete(artigo);
    }

    @Transactional(readOnly = true)
    public ArtigoConhecimento buscarPorId(UUID id) {
        Autorizacao.exigirAcessoAoSistema();
        ArtigoConhecimento artigo = artigoRepository.findById(id)
                .orElseThrow(() -> naoEncontrado(id));
        exigirMesmoCondominio(artigo, id);
        return artigo;
    }

    /**
     * Artigos publicados, do condomínio de quem pediu.
     *
     * O filtro por categoria **precisa** manter o recorte de condomínio. Antes
     * ele o substituía: pedir uma categoria chamava a consulta que só filtra
     * por categoria, e o morador recebia a base de conhecimento de todos os
     * condomínios da plataforma. A listagem sem filtro estava certa, o que
     * fazia o vazamento aparecer só quando alguém usava o seletor.
     */
    @Transactional(readOnly = true)
    public List<ArtigoConhecimento> listarPublicados(String condominioId, CategoriaArtigo categoria) {
        Autorizacao.exigirAcessoAoSistema();

        if (condominioId == null) {
            // Admin Geral: a plataforma inteira, de propósito.
            if (categoria != null) return artigoRepository.findByCategoriaAndPublicadoTrue(categoria);
            return artigoRepository.findByPublicadoTrue();
        }

        List<ArtigoConhecimento> doCondominio =
                artigoRepository.findByCondominioIdAndPublicadoTrue(condominioId);
        if (categoria == null) return doCondominio;

        return doCondominio.stream()
                .filter(a -> categoria.equals(a.getCategoria()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ArtigoConhecimento> listarTodos(String condominioId) {
        // Mesma regra dos avisos: aqui vem o não publicado. O FAQ do morador
        // usa `listarPublicados`.
        Autorizacao.exigirVisaoDeGestao("ver os artigos não publicados");
        if (condominioId != null) return artigoRepository.findByCondominioId(condominioId);
        return artigoRepository.findAll();
    }

    /** Perfil primeiro, existência depois — a mesma ordem do `AvisoService`. */
    private ArtigoConhecimento buscarParaEscrita(UUID id, String operacao) {
        Autorizacao.exigirGestaoDoCondominio(operacao);
        ArtigoConhecimento artigo = artigoRepository.findById(id)
                .orElseThrow(() -> naoEncontrado(id));
        exigirMesmoCondominio(artigo, id);
        return artigo;
    }

    private void exigirMesmoCondominio(ArtigoConhecimento artigo, UUID id) {
        if (Autorizacao.ehAdminGeral()) return;

        String meu = CondominioUtils.condominioIdEfetivo();
        if (meu == null || !meu.equals(artigo.getCondominioId())) {
            throw naoEncontrado(id);
        }
    }

    private RecursoNaoEncontradoException naoEncontrado(UUID id) {
        return new RecursoNaoEncontradoException("Artigo não encontrado com id: " + id);
    }
}
