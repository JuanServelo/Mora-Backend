package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import portaria.model.LiberacaoExcepcional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LiberacaoExcepcionalRepository extends JpaRepository<LiberacaoExcepcional, String> {

    List<LiberacaoExcepcional> findByCondominioId(String condominioId);

    /**
     * Inclui a véspera: uma liberação que vira a meia-noite ainda vale de
     * madrugada, pelo mesmo motivo que os turnos noturnos.
     */
    List<LiberacaoExcepcional> findByAuthUserIdAndDataIn(String authUserId, List<LocalDate> datas);
}
