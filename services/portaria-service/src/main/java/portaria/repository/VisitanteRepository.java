package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portaria.model.Visitante;
import portaria.model.enums.StatusAcesso;
import java.util.List;

public interface VisitanteRepository extends JpaRepository<Visitante, String> {
    List<Visitante> findByStatus(StatusAcesso status);
}
