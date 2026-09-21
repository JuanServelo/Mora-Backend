package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import portaria.model.ReservaAuditoria;

import java.util.List;

@Repository
public interface ReservaAuditoriaRepository extends JpaRepository<ReservaAuditoria, String> {

    List<ReservaAuditoria> findByReservaIdOrderByCriadoEmDesc(String reservaId);
}
