package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import portaria.model.AreaComumHorario;

import java.util.List;

@Repository
public interface AreaComumHorarioRepository extends JpaRepository<AreaComumHorario, String> {

    List<AreaComumHorario> findByAreaComumId(String areaComumId);

    void deleteByAreaComumId(String areaComumId);
}
