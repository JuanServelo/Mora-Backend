package ocorrencias.service;

import lombok.RequiredArgsConstructor;
import ocorrencias.dto.FornecedorRequest;
import ocorrencias.exception.RecursoNaoEncontradoException;
import ocorrencias.model.Fornecedor;
import ocorrencias.repository.FornecedorRepository;
import ocorrencias.security.Autorizacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FornecedorService {

    private final FornecedorRepository fornecedores;

    @Transactional(readOnly = true)
    public List<Fornecedor> listar(String condominioPedido) {
        Autorizacao.exigirVisaoDeGestao("consultar fornecedores");
        String condominioId = Autorizacao.condominioDeLeitura(condominioPedido);
        return condominioId == null
                ? fornecedores.findAllByOrderByNomeAsc()
                : fornecedores.findByCondominioIdOrderByNomeAsc(condominioId);
    }

    public Fornecedor criar(FornecedorRequest req) {
        Autorizacao.exigirSindico("cadastrar fornecedores");
        Fornecedor f = new Fornecedor();
        f.setCondominioId(Autorizacao.exigirCondominio());
        preencher(f, req);
        f.setAtivo(true);
        return fornecedores.save(f);
    }

    public Fornecedor atualizar(Long id, FornecedorRequest req) {
        Autorizacao.exigirSindico("editar fornecedores");
        Fornecedor f = doCondominio(id);
        preencher(f, req);
        if (req.ativo() != null) f.setAtivo(req.ativo());
        return fornecedores.save(f);
    }

    /** Um fornecedor do condomínio de quem pede; de outro condomínio é 404. */
    Fornecedor doCondominio(Long id) {
        String condominioId = Autorizacao.exigirCondominio();
        return fornecedores.findById(id)
                .filter(f -> f.getCondominioId().equals(condominioId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado."));
    }

    private static void preencher(Fornecedor f, FornecedorRequest req) {
        f.setNome(req.nome().trim());
        f.setServico(vazioParaNulo(req.servico()));
        f.setTelefone(vazioParaNulo(req.telefone()));
        f.setEmail(vazioParaNulo(req.email()));
        f.setDocumento(vazioParaNulo(req.documento()));
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
