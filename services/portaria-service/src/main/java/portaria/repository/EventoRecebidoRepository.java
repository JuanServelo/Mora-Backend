package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portaria.model.EventoRecebido;

public interface EventoRecebidoRepository extends JpaRepository<EventoRecebido, String> {
}
