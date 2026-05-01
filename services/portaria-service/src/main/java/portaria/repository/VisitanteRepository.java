package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portaria.model.Visitante;
import portaria.model.enums.StatusAcesso;
import java.util.List;
import java.util.Optional;

public interface VisitanteRepository extends JpaRepository<Visitante, String> {
    List<Visitante> findByStatus(StatusAcesso status);
    Optional<Visitante> findByCpf(String cpf);
}
