package org.seoulhub.backend.domain.congestion;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CongestionResponseDto {

    public enum Freshness { RECENT, DELAYED, EXPIRED, UNAVAILABLE, HISTORICAL }
    private final Freshness freshness;
    private final Long observationAgeMinutes;
    private final java.time.Instant lastCheckedAt;
    private final java.time.Instant collectedAt;
    private final String areaName;
    private final String areaCode;
    private final String category;
    private final double latitude;
    private final double longitude;
    private final boolean stale;
    private final CongestionLevel sourceCongestionLevel;
    private final CongestionLevel congestionLevel;
    private final String congestionMessage;
    private final int populationMin;
    private final int populationMax;
    private final LocalDateTime updateTime;

    public CongestionResponseDto(LatestObservation latest) { this(latest.observation(),latest.getLastCheckedAt(),java.time.Instant.now()); }
    public CongestionResponseDto(Congestion congestion) { this(congestion,null,java.time.Instant.now()); }
    public CongestionResponseDto(Congestion congestion,java.time.Instant checkedAt,java.time.Instant now) {
        this.lastCheckedAt=checkedAt;
        this.observationAgeMinutes=congestion.getUpdateTime()==null ? null : Math.max(0,java.time.Duration.between(congestion.getUpdateTime().atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant(),now).toMinutes());
        this.freshness=observationAgeMinutes==null ? Freshness.UNAVAILABLE : observationAgeMinutes>=120 ? Freshness.EXPIRED : observationAgeMinutes>=45 ? Freshness.DELAYED : Freshness.RECENT;
        this.collectedAt=congestion.getCollectedAt();
        this.areaName = congestion.getAreaName();
        var area=AreaCatalog.resolve(areaName); areaCode=area.code(); category=area.category(); latitude=area.lat(); longitude=area.lng();
        this.stale = freshness==Freshness.DELAYED || freshness==Freshness.EXPIRED;
        this.sourceCongestionLevel = congestion.getCongestionLevel();
        this.congestionLevel = congestion.getCongestionLevel();
        this.congestionMessage = congestion.getCongestionMessage();
        this.populationMin = congestion.getPopulationMin();
        this.populationMax = congestion.getPopulationMax();
        this.updateTime = congestion.getUpdateTime();
    }

    // 집계 전용 생성자
    public CongestionResponseDto(CongestionHourlyStat stat) {
        freshness=Freshness.HISTORICAL; observationAgeMinutes=null; lastCheckedAt=null;
        this.collectedAt=null;
        this.areaName = stat.getAreaName();
        var area=AreaCatalog.resolve(areaName); areaCode=area.code(); category=area.category(); latitude=area.lat(); longitude=area.lng();
        this.stale = false;
        this.sourceCongestionLevel = stat.getCongestionLevel();
        this.congestionLevel = stat.getCongestionLevel();
        this.congestionMessage = "1시간 평균 집계 데이터입니다.";
        this.populationMin = stat.getAvgPopulationMin();
        this.populationMax = stat.getAvgPopulationMax();
        this.updateTime = stat.getStatHour();
    }
}