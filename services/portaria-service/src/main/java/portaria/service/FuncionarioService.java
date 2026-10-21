package portaria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portaria.exception.OperacaoInvalidaException;
import portaria.exception.RecursoNaoEncontradoException;
import portaria.model.Funcionario;
import portaria.repository.FuncionarioRepository;
import portaria.security.AuthContext;
import portaria.security.Permissoes;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;

    public Funcionario cadastrar(Funcionario funcionario) {
        Permissoes.exigirAdmin("cadastrar funcionários");
        funcionarioRepository.findByCpf(funcionario.getCpf()).ifPresent(f -> {
            throw new OperacaoInvalidaException("Já existe um funcionário cadastrado com o CPF: " + funcionario.getCpf());
        });
        funcionario.setCondominioId(Permissoes.condominioParaGravar(funcionario.getCondominioId()));
        return funcionarioRepository.save(funcionario);
    }

    public List<Funcionario> listarTodos(String condominioId) {
        if (condominioId != null && !condominioId.isBlank()) {
            return funcionarioRepository.findByCondominioId(condominioId);
        }
        return funcionarioRepository.findAll();
    }

    public List<Funcionario> listarAtivos(String condominioId) {
        if (condominioId != null && !condominioId.isBlank()) {
            return funcionarioRepository.findByCondominioIdAndAtivo(condominioId, true);
        }
        return funcionarioRepository.findByAtivo(true);
    }

    public Funcionario buscarPorId(String id) {
        return funcionarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário não encontrado com id: " + id));
    }

    public Funcionario atualizar(String id, Funcionario dados) {
        Permissoes.exigirAdmin("alterar funcionários");
        Funcionario funcionario = buscarPorId(id);
        Permissoes.exigirMesmoCondominio(funcionario.getCondominioId());
        funcionario.setNome(dados.getNome());
        funcionario.setCargo(dados.getCargo());
        funcionario.setMatricula(dados.getMatricula());
        funcionario.setTelefone(dados.getTelefone());
        return funcionarioRepository.save(funcionario);
    }

    public void desativar(String id) {
        Permissoes.exigirAdmin("desativar funcionários");
        Funcionario funcionario = buscarPorId(id);
        Permissoes.exigirMesmoCondominio(funcionario.getCondominioId());
        funcionario.setAtivo(false);
        funcionarioRepository.save(funcionario);
    }
}
