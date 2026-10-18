package org.seoulhub.backend.domain.congestion;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface SeoulApiBudgetRepository extends JpaRepository<SeoulApiBudget,String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from SeoulApiBudget b where b.id=:id")
    Optional<SeoulApiBudget> findLocked(@Param("id") String id);
}
