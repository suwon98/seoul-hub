package org.seoulhub.backend.domain.congestion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.*;
@Entity @Table(name="congestion_latest") @Getter @NoArgsConstructor
public class LatestObservation {
    @Id @Column(length=16) private String areaCode;
    @Column(nullable=false) private String areaName;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private CongestionLevel congestionLevel;
    @Column(nullable=false,length=500) private String congestionMessage;
    @Column(nullable=false) private Integer populationMin;
    @Column(nullable=false) private Integer populationMax;
    @Column(nullable=false) private LocalDateTime updateTime;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP)
    private Instant collectedAt;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP)
    private Instant lastCheckedAt;
    public void checked(Instant time) { lastCheckedAt=time; }
    public LatestObservation(AreaCatalog.Area area,Congestion observation) { areaCode=area.code(); apply(observation); }
    public void apply(Congestion c) {
        if(updateTime!=null && !c.getUpdateTime().isAfter(updateTime)) return;
        areaName=c.getAreaName(); congestionLevel=c.getCongestionLevel(); congestionMessage=c.getCongestionMessage();
        populationMin=c.getPopulationMin(); populationMax=c.getPopulationMax(); updateTime=c.getUpdateTime(); collectedAt=c.getCollectedAt();
    }
    public Congestion observation() { return Congestion.builder().areaName(areaName).congestionLevel(congestionLevel).congestionMessage(congestionMessage).populationMin(populationMin).populationMax(populationMax).updateTime(updateTime).collectedAt(collectedAt).build(); }
}
