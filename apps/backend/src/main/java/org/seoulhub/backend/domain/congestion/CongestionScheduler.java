package org.seoulhub.backend.domain.congestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class CongestionScheduler {

    private final CongestionRepository congestionRepository;
    private final Random random = new Random();
    private final List<String> areas = List.of("강남역", "홍대입구역", "강남대로", "테헤란로", "신림역");

    @Scheduled(fixedDelay = 10000)
    public void generateSubwayCongestionData() {
        for (String area : areas) {
            int popMin = 15000 + random.nextInt(10000);
            int popMax = popMin + random.nextInt(15000);

            CongestionLevel level = CongestionLevel.values()[random.nextInt(CongestionLevel.values().length)];
            String message = String.format("현재 %s 인근은 유동인구 약 %d명 수준으로 %s 상태입니다.",
                    area, (popMin + popMax) / 2, level.name());

            Congestion congestion = Congestion.builder()
                    .areaName(area)
                    .congestionLevel(level)
                    .congestionMessage(message)
                    .populationMin(popMin)
                    .populationMax(popMax)
                    .updateTime(LocalDateTime.now())
                    .build();

            congestionRepository.save(congestion);
        }
        log.info("[스케줄러] 서울 주요 거점 {}개소 실시간 혼잡도 적재 완료", areas.size());
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanUpOldCongestionData() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        try {
            congestionRepository.deleteOlderThan(threshold);
            log.info("[데이터 청소 배치] {} 시점 이전의 만료된 과거 혼잡도 데이터 벌크 삭제 완료", threshold);
        } catch (Exception e) {
            log.error("[데이터 청소 배치] 가동 중 에러 발생: {}", e.getMessage());
        }
    }
}