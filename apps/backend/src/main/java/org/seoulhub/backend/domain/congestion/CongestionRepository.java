package org.seoulhub.backend.domain.congestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CongestionRepository extends JpaRepository<Congestion, Long> {

    Optional<Congestion> findTopByAreaNameOrderByUpdateTimeDesc(String areaName);

    @Modifying
    @Transactional
    @Query("DELETE FROM Congestion c WHERE c.updateTime < :threshold")
    void deleteOlderThan(@Param("threshold")LocalDateTime threshold);
}
