package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portaria.model.Chave;
import java.util.List;

public interface ChaveRepository extends JpaRepository<Chave, String> {
    List<Chave> findByDisponivel(boolean disponivel);
    List<Chave> findByCondominioId(String condominioId);
    List<Chave> findByCondominioIdAndDisponivel(String condominioId, boolean disponivel);
}
