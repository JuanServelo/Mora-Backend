package portaria.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import portaria.model.Visitante;
import portaria.model.enums.StatusAcesso;
import java.util.List;
import java.util.Optional;

public interface VisitanteRepository extends JpaRepository<Visitante, String> {
    List<Visitante> findByStatus(StatusAcesso status);
    Optional<Visitante> findByCpf(String cpf);
    List<Visitante> findByCondominioId(String condominioId);
    Page<Visitante> findByCondominioId(String condominioId, Pageable pageable);
    List<Visitante> findByCondominioIdAndStatus(String condominioId, StatusAcesso status);
    Page<Visitante> findByCondominioIdAndStatus(String condominioId, StatusAcesso status, Pageable pageable);
    Page<Visitante> findByStatus(StatusAcesso status, Pageable pageable);
}
