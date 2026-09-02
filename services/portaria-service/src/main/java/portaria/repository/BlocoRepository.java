package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import portaria.model.Bloco;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlocoRepository extends JpaRepository<Bloco, UUID> {
    List<Bloco> findByAtivo(boolean ativo);
    Optional<Bloco> findByNome(String nome);

    List<Bloco> findByCondominioId(String condominioId);
    List<Bloco> findByCondominioIdAndAtivo(String condominioId, boolean ativo);
    Optional<Bloco> findByNomeAndCondominioId(String nome, String condominioId);
}
