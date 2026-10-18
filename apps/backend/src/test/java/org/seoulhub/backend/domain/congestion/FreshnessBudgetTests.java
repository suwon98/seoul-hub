package org.seoulhub.backend.domain.congestion;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
class FreshnessBudgetTests {
    @Test void ageBoundariesPreserveObservationAndIgnoreRecentCheck() {
        var now=Instant.parse("2026-10-18T12:00:00Z");
        for(int age : new int[]{30,44,45,119,120,300}) {
            var row=Congestion.builder().populationMin(0).populationMax(0).areaName("강남역").congestionLevel(CongestionLevel.RED)
                .updateTime(LocalDateTime.ofInstant(now.minusSeconds(age*60L),ZoneId.of("Asia/Seoul"))).build();
            var dto=new CongestionResponseDto(row,now,now);
            assertThat(dto.getObservationAgeMinutes()).isEqualTo(age);
            assertThat(dto.getCongestionLevel()).isEqualTo(CongestionLevel.RED);
            assertThat(dto.getFreshness()).isEqualTo(age>=120 ? CongestionResponseDto.Freshness.EXPIRED : age>=45 ? CongestionResponseDto.Freshness.DELAYED : CongestionResponseDto.Freshness.RECENT);
        }
        var missing=new CongestionResponseDto(Congestion.builder().populationMin(0).populationMax(0).areaName("강남역").congestionLevel(CongestionLevel.UNKNOWN).build(),null,now);
        assertThat(missing.getFreshness()).isEqualTo(CongestionResponseDto.Freshness.UNAVAILABLE);
        assertThat(missing.getObservationAgeMinutes()).isNull();
    }
    @Test void budgetPacesRequestsAndCapsKoreanCalendarDay() {
        var budget=new SeoulApiBudget("population");
        var now=Instant.parse("2026-10-18T14:58:00Z");
        assertThat(budget.acquire(now,2,1000)).isTrue();
        assertThat(budget.acquire(now.plusMillis(999),2,1000)).isFalse();
        assertThat(budget.acquire(now.plusSeconds(1),2,1000)).isTrue();
        assertThat(budget.acquire(now.plusSeconds(60),2,1000)).isFalse();
        assertThat(budget.acquire(Instant.parse("2026-10-18T15:00:00Z"),2,1000)).isTrue();
        assertThat(budget.getUsed()).isEqualTo(1);
        assertThat(budget.getBudgetDate()).isEqualTo(LocalDate.of(2026,10,19));
    }
    @Test void intervalRespectsBothBudgetAndSourceCadence() {
        var repo=mock(SeoulApiBudgetRepository.class);
        assertThat(new SeoulCollectionPolicy(repo,900,0).getRequestDelayMs()).isEqualTo(1000);
        assertThat(new SeoulCollectionPolicy(repo,35000,0).getRequestDelayMs()).isEqualTo(1000);
        assertThat(new SeoulCollectionPolicy(repo,900,120000).getRequestDelayMs()).isEqualTo(120000);
    }
}
