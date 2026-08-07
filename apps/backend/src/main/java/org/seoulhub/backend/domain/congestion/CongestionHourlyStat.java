package org.seoulhub.backend.domain.congestion;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "congestion_hourly_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CongestionHourlyStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String areaName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CongestionLevel congestionLevel;

    @Column(nullable = false)
    private Integer avgPopulationMin;

    @Column(nullable = false)
    private Integer avgPopulationMax;

    @Column(nullable = false)
    private LocalDateTime statHour;

    @Builder
    public CongestionHourlyStat(String areaName, CongestionLevel congestionLevel, Integer avgPopulationMin, Integer avgPopulationMax, LocalDateTime statHour) {
        this.areaName = areaName;
        this.congestionLevel = congestionLevel;
        this.avgPopulationMin = avgPopulationMin;
        this.avgPopulationMax = avgPopulationMax;
        this.statHour = statHour;
    }
}