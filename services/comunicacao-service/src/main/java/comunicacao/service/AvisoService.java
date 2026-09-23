package comunicacao.service;

import comunicacao.exception.RecursoNaoEncontradoException;
import comunicacao.model.Aviso;
import comunicacao.repository.AvisoRepository;
import comunicacao.security.AuthContext;
import comunicacao.security.Autorizacao;
import comunicacao.security.CondominioUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Os comunicados da administração.
 *
 * As checagens de acesso moram aqui, e não nos controllers, por um motivo
 * prático: um controller novo — ou um endpoint acrescentado a um existente —
 * passaria por cima delas sem que ninguém percebesse. No serviço, quem chama
 * não tem como contornar.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AvisoService {

    private final AvisoRepository avisoRepository;

    public Aviso criar(Aviso aviso) {
        Autorizacao.exigirGestaoDoCondominio("publicar comunicados");

        var claims = AuthContext.get();
        // O condomínio sai da claim do token, nunca do corpo. Aceitar do corpo
        // deixaria o síndico publicar em nome de outro condomínio escrevendo
        // outro id — e o campo chega preenchido, porque o controller recebe a
        // entidade crua.
        aviso.setCondominioId(CondominioUtils.condominioIdEfetivo());
        if (aviso.getAutor() == null && claims != null) {
            aviso.setAutor(claims.email());
        }
        return avisoRepository.save(aviso);
    }

    public Aviso publicar(UUID id) {
        Aviso aviso = buscarParaEscrita(id, "publicar comunicados");
        aviso.setPublicado(true);
        aviso.setAtualizadoEm(LocalDateTime.now());
        return avisoRepository.save(aviso);
    }

    public Aviso encerrar(UUID id) {
        Aviso aviso = buscarParaEscrita(id, "encerrar comunicados");
        aviso.setDataFim(LocalDate.now().minusDays(1));
        aviso.setAtualizadoEm(LocalDateTime.now());
        return avisoRepository.save(aviso);
    }

    public Aviso atualizar(UUID id, Aviso dados) {
        Aviso aviso = buscarParaEscrita(id, "editar comunicados");
        aviso.setTitulo(dados.getTitulo());
        aviso.setMensagem(dados.getMensagem());
        aviso.setDataInicio(dados.getDataInicio());
        aviso.setDataFim(dados.getDataFim());
        aviso.setPublicoAlvo(dados.getPublicoAlvo());
        aviso.setImagemUrl(dados.getImagemUrl());
        // `publicado` entra junto porque as duas telas de comunicado — a tela
        // própria e a aba dentro de Conhecimento — publicam por uma caixa de
        // seleção no formulário, não pela rota `/publicar`. Sem esta linha a
        // caixa não fazia nada, e o síndico marcava, salvava e via o aviso
        // continuar como rascunho sem erro nenhum.
        //
        // Copiar é seguro porque as duas mandam o campo sempre, inclusive ao
        // editar. Se alguma tela parar de mandar, o valor chega `false` (é o
        // default da entidade) e o aviso despublica — por isso o campo não pode
        // virar opcional no formulário sem mexer aqui também.
        aviso.setPublicado(dados.isPublicado());
        aviso.setAtualizadoEm(LocalDateTime.now());
        return avisoRepository.save(aviso);
    }

    public void excluir(UUID id) {
        Aviso aviso = buscarParaEscrita(id, "excluir comunicados");
        avisoRepository.delete(aviso);
    }

    /**
     * Um aviso, se ele for visível para quem está pedindo.
     *
     * Condomínio alheio responde **404, e não 403**: "existe, mas não é seu" já
     * confirma que existe, e um id de aviso é fácil de varrer.
     */
    @Transactional(readOnly = true)
    public Aviso buscarPorId(UUID id) {
        Autorizacao.exigirAcessoAoSistema();
        Aviso aviso = avisoRepository.findById(id)
                .orElseThrow(() -> naoEncontrado(id));
        exigirMesmoCondominio(aviso, id);
        return aviso;
    }

    @Transactional(readOnly = true)
    public List<Aviso> listarTodos(String condominioId) {
        // Esta é a visão da administração: devolve **também o que não foi
        // publicado**. Para o morador existe `listarAtivos`, que filtra por
        // `publicado = true` e pela vigência. Sem esta barreira, o rascunho de
        // um comunicado aparecia para quem ele ainda não deveria alcançar.
        Autorizacao.exigirVisaoDeGestao("ver os comunicados não publicados");
        if (condominioId == null) return avisoRepository.findAllByOrderByCriadoEmDesc();
        return avisoRepository.findByCondominioIdOrderByCriadoEmDesc(condominioId);
    }

    @Transactional(readOnly = true)
    public List<Aviso> listarAtivos(String condominioId) {
        Autorizacao.exigirAcessoAoSistema();
        if (condominioId == null) return avisoRepository.findTodosAtivos(LocalDate.now());
        return avisoRepository.findAtivos(condominioId, LocalDate.now());
    }

    /**
     * O aviso que esta requisição pode alterar — ou o erro certo, na ordem certa.
     *
     * Perfil primeiro, existência depois. Invertido, um morador descobriria
     * quais ids existem pela diferença entre 404 e 403 antes de levar o 403.
     */
    private Aviso buscarParaEscrita(UUID id, String operacao) {
        Autorizacao.exigirGestaoDoCondominio(operacao);
        Aviso aviso = avisoRepository.findById(id)
                .orElseThrow(() -> naoEncontrado(id));
        exigirMesmoCondominio(aviso, id);
        return aviso;
    }

    /**
     * Recusa aviso de outro condomínio.
     *
     * Sem isto, o síndico do condomínio A editava e excluía o comunicado do B
     * sabendo o id — as rotas por id não olhavam o condomínio, só a listagem
     * olhava. O Admin Geral passa, porque o alcance dele é a plataforma inteira.
     */
    private void exigirMesmoCondominio(Aviso aviso, UUID id) {
        if (Autorizacao.ehAdminGeral()) return;

        String meu = CondominioUtils.condominioIdEfetivo();
        if (meu == null || !meu.equals(aviso.getCondominioId())) {
            throw naoEncontrado(id);
        }
    }

    private RecursoNaoEncontradoException naoEncontrado(UUID id) {
        return new RecursoNaoEncontradoException("Aviso não encontrado com id: " + id);
    }
}
