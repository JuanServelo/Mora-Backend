package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import portaria.dto.ArtigoConhecimentoRequestDTO;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.ArtigoConhecimento;
import portaria.model.enums.CategoriaArtigo;
import portaria.repository.ArtigoConhecimentoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArtigoConhecimentoService {

    private final ArtigoConhecimentoRepository repository;

    public ArtigoConhecimento criar(ArtigoConhecimentoRequestDTO request) {
        ArtigoConhecimento artigo = new ArtigoConhecimento();
        artigo.setTitulo(request.getTitulo());
        artigo.setConteudo(request.getConteudo());
        artigo.setCategoria(request.getCategoria());
        artigo.setAutor(request.getAutor());
        artigo.setPublicado(request.isPublicado());
        artigo.setCriadoEm(LocalDateTime.now());
        artigo.setAtualizadoEm(LocalDateTime.now());
        return repository.save(artigo);
    }

    public ArtigoConhecimento atualizar(UUID id, ArtigoConhecimentoRequestDTO request) {
        ArtigoConhecimento artigo = buscarPorId(id);
        artigo.setTitulo(request.getTitulo());
        artigo.setConteudo(request.getConteudo());
        artigo.setCategoria(request.getCategoria());
        artigo.setAutor(request.getAutor());
        artigo.setPublicado(request.isPublicado());
        artigo.setAtualizadoEm(LocalDateTime.now());
        return repository.save(artigo);
    }

    public void excluir(UUID id) {
        buscarPorId(id);
        repository.deleteById(id);
    }

    public List<ArtigoConhecimento> listarTodos() {
        return repository.findAll();
    }

    public List<ArtigoConhecimento> listarPublicados() {
        return repository.findByPublicado(true);
    }

    public List<ArtigoConhecimento> listarPorCategoria(CategoriaArtigo categoria) {
        return repository.findByCategoria(categoria);
    }

    public List<ArtigoConhecimento> listarPublicadosPorCategoria(CategoriaArtigo categoria) {
        return repository.findByCategoriaAndPublicado(categoria, true);
    }

    public List<ArtigoConhecimento> buscarPorTitulo(String titulo) {
        return repository.findByTituloContainingIgnoreCase(titulo);
    }

    public ArtigoConhecimento buscarPorId(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Artigo não encontrado com id: " + id));
    }
}
