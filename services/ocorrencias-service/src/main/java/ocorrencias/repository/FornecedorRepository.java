package ocorrencias.repository;

import ocorrencias.model.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    List<Fornecedor> findByCondominioIdOrderByNomeAsc(String condominioId);

    List<Fornecedor> findAllByOrderByNomeAsc();
}
