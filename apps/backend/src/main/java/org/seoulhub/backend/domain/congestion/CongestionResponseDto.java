package org.seoulhub.backend.domain.congestion;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CongestionResponseDto {

    private final String areaName;
    private final CongestionLevel congestionLevel;
    private final String congestionMessage;
    private final Integer populationMin;
    private final Integer populationMax;
    private final LocalDateTime updateTime;

    public CongestionResponseDto(Congestion congestion) {
        this.areaName = congestion.getAreaName();
        this.congestionLevel = congestion.getCongestionLevel();
        this.congestionMessage = congestion.getCongestionMessage();
        this.populationMin = congestion.getPopulationMin();
        this.populationMax = congestion.getPopulationMax();
        this.updateTime = congestion.getUpdateTime();
    }
}
