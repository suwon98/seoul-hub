package org.seoulhub.backend.domain.congestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "seoul.collection.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class CongestionScheduler {

    private final CongestionRepository congestionRepository;
    private final CongestionHourlyStatRepository hourlyStatRepository;
    private final CongestionService congestionService;
    private final List<String> areas = CongestionService.AREAS;
    private final SeoulCollectionPolicy collectionPolicy;
    private final ViewportRefreshQueue refreshQueue;

    @Scheduled(initialDelay=5000, fixedDelay=1000)
    public void generateSubwayCongestionData() {
        if(collectionPolicy.status().usedToday()>=collectionPolicy.getDailyBudget()) return;
        refreshQueue.next().ifPresent(area -> {
            try { congestionService.collect(area); }
            catch(RuntimeException e) { log.warn("Collection transaction failed: area={}, type={}",area,e.getClass().getSimpleName()); }
        });
    }

    // 지연 도착한 관측도 반영하도록 5분마다 최근 24개 완료 시간을 다시 집계합니다.
    @Scheduled(cron = "0 */5 * * * *", zone="Asia/Seoul")
    @Transactional
    public void aggregateHourlyStats() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        for (String area : areas) {
            var byHour=congestionRepository.findByAreaNameInAndUpdateTimeGreaterThanEqualAndUpdateTimeLessThanOrderByUpdateTimeAsc(AreaCatalog.namesFor(area),now.minusHours(24),now).stream()
                .collect(java.util.stream.Collectors.groupingBy(c -> c.getUpdateTime().truncatedTo(ChronoUnit.HOURS)));
            for (var entry:byHour.entrySet()) {
                var startHour=entry.getKey();
                // Aliases retained from older versions must not double-count the same observation.
                var unique=new java.util.TreeMap<LocalDateTime,Congestion>();
                for(var record:entry.getValue()) unique.put(record.getUpdateTime(),record);
                var records=List.copyOf(unique.values());
                int avgMin=(int)records.stream().mapToInt(Congestion::getPopulationMin).average().orElse(0);
                int avgMax=(int)records.stream().mapToInt(Congestion::getPopulationMax).average().orElse(0);
                var level=records.getLast().getCongestionLevel();
                var stat=hourlyStatRepository.findByAreaNameAndStatHour(area,startHour).orElseGet(() -> CongestionHourlyStat.builder().areaName(area).congestionLevel(level).avgPopulationMin(avgMin).avgPopulationMax(avgMax).statHour(startHour).build());
                stat.updateObservation(level,avgMin,avgMax);
                hourlyStatRepository.save(stat);
            }
        }
        log.info("Hourly observations aggregated through {}", now);
    }

    // 집계와 지연 수신을 위해 원시 관측은 48시간 보관합니다.
    @Scheduled(initialDelay=60000, fixedDelay = 3600000)
    public void cleanUpOldCongestionData() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(48);
        try {
            congestionRepository.deleteOlderThan(threshold);
            hourlyStatRepository.deleteByStatHourBefore(LocalDateTime.now().minusDays(30));
            log.info("🧹 [데이터 청소 배치] {} 시점 이전의 만료된 과거 혼잡도 데이터 벌크 삭제 완료", threshold);
        } catch (Exception e) {
            log.error("Observation cleanup failed: errorType={}", e.getClass().getSimpleName());
        }
    }

}
