package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portaria.model.Morador;
import java.util.List;
import java.util.Optional;

public interface MoradorRepository extends JpaRepository<Morador, String> {
    Optional<Morador> findByCpf(String cpf);
    List<Morador> findByAtivo(boolean ativo);
}
