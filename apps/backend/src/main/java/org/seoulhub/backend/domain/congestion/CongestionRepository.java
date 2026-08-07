package org.seoulhub.backend.domain.congestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CongestionRepository extends JpaRepository<Congestion, Long> {

    Optional<Congestion> findTopByAreaNameOrderByUpdateTimeDesc(String areaName);

    // 거점별 최신 혼잡도 데이터 1건씩 전체 집계
    @Query("SELECT c FROM Congestion c WHERE c.id IN (SELECT MAX(c2.id) FROM Congestion c2 GROUP BY c2.areaName)")
    List<Congestion> findAllLatestCongestion();

    // 특정 거점의 최근 과거 히스토리 기록 시간 오름차순 조회
    List<Congestion> findTop20ByAreaNameOrderByUpdateTimeAsc(String areaName);

    @Modifying
    @Transactional
    @Query("DELETE FROM Congestion c WHERE c.updateTime < :threshold")
    void deleteOlderThan(@Param("threshold") LocalDateTime threshold);
}