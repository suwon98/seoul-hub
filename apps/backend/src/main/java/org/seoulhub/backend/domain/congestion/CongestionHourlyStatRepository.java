package org.seoulhub.backend.domain.congestion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CongestionHourlyStatRepository extends JpaRepository<CongestionHourlyStat, Long> {

    // 특정 거점의 최근 24시간 집계 데이터 시간 오름차순 조회
    List<CongestionHourlyStat> findTop24ByAreaNameOrderByStatHourAsc(String areaName);
}