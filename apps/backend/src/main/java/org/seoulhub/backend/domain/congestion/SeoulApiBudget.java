package org.seoulhub.backend.domain.congestion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.*;
@Entity @Table(name="seoul_api_budget") @Getter @NoArgsConstructor
public class SeoulApiBudget {
    @Id @Column(length=32) private String id;
    private LocalDate budgetDate;
    private long used;
    private int nextAreaIndex;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP)
    private Instant nextAllowedAt;
    public SeoulApiBudget(String id) { this.id=id; }
    public boolean acquire(Instant now,long limit,long delayMs) {
        var today=now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
        if(!today.equals(budgetDate)) { budgetDate=today; used=0; }
        if(used>=limit || (nextAllowedAt!=null && now.isBefore(nextAllowedAt))) return false;
        used++; nextAreaIndex=(nextAreaIndex+1)%AreaCatalog.ALL.size(); nextAllowedAt=now.plusMillis(delayMs); return true;
    }
}
