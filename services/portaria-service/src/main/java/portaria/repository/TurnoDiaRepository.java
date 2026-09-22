package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import portaria.model.TurnoDia;

import java.util.List;

@Repository
public interface TurnoDiaRepository extends JpaRepository<TurnoDia, String> {

    List<TurnoDia> findByJornadaId(String jornadaId);

    void deleteByJornadaId(String jornadaId);
}
