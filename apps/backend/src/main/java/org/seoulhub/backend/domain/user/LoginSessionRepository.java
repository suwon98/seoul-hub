package org.seoulhub.backend.domain.user;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
public interface LoginSessionRepository extends JpaRepository<LoginSession,String> {
    long deleteByEmail(String email);
    long deleteByExpiresAtBefore(Instant time);
}
