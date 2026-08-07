package org.seoulhub.backend.domain.congestion;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CongestionResponseDto {

    private final String areaName;
    private final CongestionLevel congestionLevel;
    private final String congestionMessage;
    private final int populationMin;
    private final int populationMax;
    private final LocalDateTime updateTime;

    public CongestionResponseDto(Congestion congestion) {
        this.areaName = congestion.getAreaName();
        this.congestionLevel = congestion.getCongestionLevel();
        this.congestionMessage = congestion.getCongestionMessage();
        this.populationMin = congestion.getPopulationMin();
        this.populationMax = congestion.getPopulationMax();
        this.updateTime = congestion.getUpdateTime();
    }

    // 집계 전용 생성자
    public CongestionResponseDto(CongestionHourlyStat stat) {
        this.areaName = stat.getAreaName();
        this.congestionLevel = stat.getCongestionLevel();
        this.congestionMessage = "1시간 평균 집계 데이터입니다.";
        this.populationMin = stat.getAvgPopulationMin();
        this.populationMax = stat.getAvgPopulationMax();
        this.updateTime = stat.getStatHour();
    }
}