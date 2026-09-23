package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import portaria.model.VeiculoPessoa;

import java.util.List;
import java.util.Optional;

@Repository
public interface VeiculoPessoaRepository extends JpaRepository<VeiculoPessoa, String> {

    List<VeiculoPessoa> findByVeiculoId(String veiculoId);

    List<VeiculoPessoa> findByVeiculoIdIn(List<String> veiculoIds);

    /** Base da listagem do morador: os veículos que a unidade dele enxerga. */
    List<VeiculoPessoa> findByUnidadeId(String unidadeId);

    Optional<VeiculoPessoa> findByVeiculoIdAndPessoaId(String veiculoId, String pessoaId);

    boolean existsByVeiculoIdAndPessoaId(String veiculoId, String pessoaId);

    long countByVeiculoId(String veiculoId);

    void deleteByVeiculoId(String veiculoId);
}
