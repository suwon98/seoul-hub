package org.seoulhub.backend.domain.congestion;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "congestion_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Congestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String areaName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CongestionLevel congestionLevel;

    @Column(nullable = false, length = 500)
    private String congestionMessage;

    @Column(nullable = false)
    private Integer populationMin;

    @Column(nullable = false)
    private Integer populationMax;

    @Column(nullable = false)
    private LocalDateTime updateTime;

    @Builder
    public Congestion(String areaName, CongestionLevel congestionLevel, String congestionMessage,
                      Integer populationMin, Integer populationMax, LocalDateTime updateTime) {
        this.areaName = areaName;
        this.congestionLevel = congestionLevel;
        this.congestionMessage = congestionMessage;
        this.populationMin = populationMin;
        this.populationMax = populationMax;
        this.updateTime = updateTime;
    }
}
