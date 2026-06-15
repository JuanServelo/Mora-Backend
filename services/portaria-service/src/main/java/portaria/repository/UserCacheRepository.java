package portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portaria.model.UserCache;

public interface UserCacheRepository extends JpaRepository<UserCache, Long> {
}
