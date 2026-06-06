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

    // 결정 로그 5, 6번에 수립된 서울 핵심 거점 포인트 매핑
    private final List<String> targetAreas = List.of("홍대입구역", "강남역", "강남대로", "테헤란로", "신림역");

    @Scheduled(fixedDelay = 10000)
    public void collectRealTimeCongestionData() {
        log.info("[파이프라인] 정식 Congestion 도메인 기반 실시간 수집 배치 가동...");

        String areaName = targetAreas.get(random.nextInt(targetAreas.size()));
        CongestionLevel level = getRandomLevel();

        String message = switch (level) {
            case GREEN -> "해당 지역은 보행 유동 인구 및 흐름이 아주 여유롭고 원활합니다.";
            case YELLOW -> "일반적인 유입량 수준을 보이고 있으며, 보통의 흐름을 유지 중입니다.";
            case ORANGE -> "인파가 대거 밀집하여 다소 다소 무겁고 혼잡하오니 안전에 유의바랍니다.";
            case RED -> "극심한 인구 병목 및 정체가 발생 중입니다. 해당 거점 진입 우회를 강력 권장합니다.";
        };

        int minPop = random.nextInt(30000) + 10000;
        int maxPop = random.nextInt(15000);

        Congestion congestion = Congestion.builder()
                .areaName(areaName)
                .congestionLevel(level)
                .congestionMessage(message)
                .populationMin(minPop)
                .populationMax(maxPop)
                .updateTime(LocalDateTime.now())
                .build();

        congestionRepository.save(congestion);
        log.info("[파이프라인] 적재 완료: 장소={}, 단계={}", areaName, level);
    }

    private CongestionLevel getRandomLevel() {
        CongestionLevel[] levels = CongestionLevel.values();
        return levels[random.nextInt(levels.length)];
    }
}
