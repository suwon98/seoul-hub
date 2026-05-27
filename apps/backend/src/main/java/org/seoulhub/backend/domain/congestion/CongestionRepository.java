package org.seoulhub.backend.domain.congestion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CongestionRepository extends JpaRepository<Congestion, Long> {
    Optional<Congestion> findTopByAreaNameOrderByUpdateTimeDesc(String areaName);
}
