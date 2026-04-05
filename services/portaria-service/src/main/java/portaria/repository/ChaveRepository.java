package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portaria.model.Chave;
import java.util.List;

public interface ChaveRepository extends JpaRepository<Chave, String> {
    List<Chave> findByDisponivel(boolean disponivel);
}
