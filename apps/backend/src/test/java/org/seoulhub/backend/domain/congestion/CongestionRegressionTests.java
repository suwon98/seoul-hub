package org.seoulhub.backend.domain.congestion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CongestionRegressionTests {
    @Autowired CongestionRepository records;
    @Autowired CongestionHourlyStatRepository stats;
    @Autowired CongestionService service;
    @Autowired LatestObservationRepository latest;
    @Autowired SeoulCollectionPolicy policy;
    @Autowired ViewportRefreshQueue queue;

    @Test void historyReturnsLatest24InChronologicalOrder() {
        LocalDateTime start = LocalDateTime.now().minusHours(30);
        for (int i = 0; i < 30; i++) stats.save(CongestionHourlyStat.builder().areaName("강남역")
                .congestionLevel(CongestionLevel.GREEN).avgPopulationMin(i).avgPopulationMax(i + 10).statHour(start.plusHours(i)).build());
        var history = service.getCongestionHistory("강남역");
        assertThat(history).hasSize(24);
        assertThat(history.getFirst().getPopulationMin()).isEqualTo(6);
        assertThat(history.getLast().getPopulationMin()).isEqualTo(29);
    }

    @Test void aggregationUsesEntirePreviousHourAndExcludesOtherHours() {
        LocalDateTime hour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        for (int i = 0; i < 30; i++) save("강남역", hour.minusHours(1).plusMinutes(i), i < 20 ? 100 : 400);
        save("강남역", hour.minusHours(2), 9000);
        save("강남역", hour, 9000);
        new CongestionScheduler(records, stats, service, policy, queue).aggregateHourlyStats();
        var result = stats.findTop24ByAreaNameOrderByStatHourDesc("강남역");
        assertThat(result).hasSize(2);
        assertThat(result.getFirst().getAvgPopulationMin()).isEqualTo(200);
        assertThat(result.getFirst().getStatHour()).isEqualTo(hour.minusHours(1));
    }

    @Test void lateObservationsUpdateExistingHourlyAverageWithoutDuplicatingIt() {
        LocalDateTime hour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).minusHours(1);
        var scheduler = new CongestionScheduler(records, stats, service, policy, queue);
        save("강남역", hour.plusMinutes(5), 100);
        scheduler.aggregateHourlyStats();
        save("강남역", hour.plusMinutes(50), 300);
        scheduler.aggregateHourlyStats();
        scheduler.aggregateHourlyStats();
        var result = stats.findTop24ByAreaNameOrderByStatHourDesc("강남역");
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getAvgPopulationMin()).isEqualTo(200);
    }

    @Test void staleObservationIsNotPresentedAsLive() {
        var row=Congestion.builder().areaName("강남역").congestionLevel(CongestionLevel.GREEN).congestionMessage("test").populationMin(1000).populationMax(1000).updateTime(LocalDateTime.now().minusHours(1)).build();
        latest.save(new LatestObservation(AreaCatalog.resolve("강남역"),row));
        assertThat(service.getCongestionByArea("강남역").getCongestionLevel()).isEqualTo(CongestionLevel.GREEN);
        assertThat(service.getCongestionByArea("강남역").isStale()).isTrue();

    }

    @Test void cleanupRetainsLatestAndAllOfficialAreasRemainVisible() {
        var row=Congestion.builder().areaName("강남역").congestionLevel(CongestionLevel.GREEN).congestionMessage("test").populationMin(123).populationMax(234).updateTime(LocalDateTime.now().minusDays(3)).build();
        records.save(row); latest.save(new LatestObservation(AreaCatalog.resolve("강남역"),row));
        new CongestionScheduler(records,stats,service,policy,queue).cleanUpOldCongestionData();
        assertThat(records.findTopByAreaNameOrderByUpdateTimeDesc("강남역")).isEmpty();
        assertThat(service.getCongestionByArea("강남역").getPopulationMax()).isEqualTo(234);
        assertThat(service.getAllLatestCongestion()).hasSize(121);
    }
    @Test void olderObservationCannotReplaceLatest() {
        var row=Congestion.builder().areaName("강남역").congestionLevel(CongestionLevel.GREEN).congestionMessage("new").populationMin(100).populationMax(200).updateTime(LocalDateTime.now()).build();
        var current=new LatestObservation(AreaCatalog.resolve("강남역"),row);
        current.apply(Congestion.builder().areaName("강남역").congestionLevel(CongestionLevel.RED).congestionMessage("old").populationMin(1).populationMax(2).updateTime(LocalDateTime.now().minusHours(1)).build());
        assertThat(current.getPopulationMax()).isEqualTo(200);
    }
    @Test void catalogHasUniqueCodesAndSeoulCoordinates() {
        assertThat(AreaCatalog.ALL).hasSize(121);
        assertThat(AreaCatalog.ALL.stream().map(AreaCatalog.Area::code).distinct().count()).isEqualTo(121);
        assertThat(AreaCatalog.ALL).allMatch(a -> a.lat()>37 && a.lat()<38 && a.lng()>126 && a.lng()<128);
    }

    private void save(String area, LocalDateTime time, int population) {
        records.save(Congestion.builder().areaName(area).congestionLevel(CongestionLevel.GREEN)
                .congestionMessage("test").populationMin(population).populationMax(population).updateTime(time).build());
    }
}
