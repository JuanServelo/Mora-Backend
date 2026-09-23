package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import portaria.model.FuncionarioAuditoria;

import java.util.List;

@Repository
public interface FuncionarioAuditoriaRepository extends JpaRepository<FuncionarioAuditoria, String> {

    List<FuncionarioAuditoria> findByAuthUserIdOrderByCriadoEmDesc(String authUserId);

    List<FuncionarioAuditoria> findByCondominioIdOrderByCriadoEmDesc(String condominioId);
}
