package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import portaria.model.FuncionarioJornada;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuncionarioJornadaRepository extends JpaRepository<FuncionarioJornada, String> {

    Optional<FuncionarioJornada> findByAuthUserIdAndCondominioId(String authUserId, String condominioId);

    Optional<FuncionarioJornada> findByAuthUserId(String authUserId);

    List<FuncionarioJornada> findByCondominioId(String condominioId);
}
