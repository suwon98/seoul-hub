package org.seoulhub.backend.domain.congestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CongestionScheduler {

    private final CongestionRepository congestionRepository;
    private final CongestionHourlyStatRepository hourlyStatRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClient webClient = WebClient.builder()
            .baseUrl("http://openapi.seoul.go.kr:8088")
            .build();

    private final List<String> areas = List.of("강남역", "홍대입구역", "신림역");

    @Value("${seoul.api.key}")
    private String apiKey;

    // 10초마다 실제 서울시 서버를 타격하여 '실시간 인구데이터'를 가져와 DB에 적재합니다.
    @Scheduled(fixedDelay = 10000)
    public void generateSubwayCongestionData() {
        for (String area : areas) {
            try {
                String responseBody = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/{key}/json/citydata_ppltn/1/1/{area}")
                                .build(apiKey, area))
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                JsonNode root = objectMapper.readTree(responseBody);

                JsonNode arrayNode = root.path("SeoulRtd.citydata_ppltn");
                JsonNode rowNode = (arrayNode.isMissingNode() || !arrayNode.isArray()) ? null : arrayNode.get(0);

                if (rowNode == null) {
                    log.warn("⚠️ [공공 스케줄러] {} 거점의 인구 데이터 데이터가 응답에 포함되지 않았습니다. (원본 응답: {})", area, responseBody);
                    continue;
                }

                String apiLevel = rowNode.path("AREA_CONGEST_LVL").asText("보통");
                String apiMessage = rowNode.path("AREA_CONGEST_MSG").asText("");
                int popMin = rowNode.path("AREA_PPLTN_MIN").asInt(0);
                int popMax = rowNode.path("AREA_PPLTN_MAX").asInt(0);

                CongestionLevel level = convertToDomainLevel(apiLevel);

                Congestion congestion = Congestion.builder()
                        .areaName(area)
                        .congestionLevel(level)
                        .congestionMessage(apiMessage)
                        .populationMin(popMin)
                        .populationMax(popMax)
                        .updateTime(LocalDateTime.now())
                        .build();

                congestionRepository.save(congestion);
                log.info("[공공 스케줄러] 라이브 데이터 적재 성공 - 거점: {}, 등급: {}, 인구수: {}명~{}명",
                        area, level.name(), popMin, popMax);

            } catch (Exception e) {
                log.error("[공공 스케줄러] 통신 중 서버 결함 발생 (거점: {}): {}", area, e.getMessage());
            }
        }
    }

    // 매시 정각마다 최근 1시간 원시 데이터를 요약 집계하여 시간별 통계 테이블에 영구 적재
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void aggregateHourlyStats() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        LocalDateTime startHour = now.minusHours(1);

        for (String area : areas) {
            List<Congestion> records = congestionRepository.findTop20ByAreaNameOrderByUpdateTimeAsc(area);
            if (records.isEmpty()) continue;

            int avgMin = (int) records.stream().mapToInt(Congestion::getPopulationMin).average().orElse(0);
            int avgMax = (int) records.stream().mapToInt(Congestion::getPopulationMax).average().orElse(0);
            CongestionLevel latestLevel = records.get(records.size() - 1).getCongestionLevel();

            CongestionHourlyStat stat = CongestionHourlyStat.builder()
                    .areaName(area)
                    .congestionLevel(latestLevel)
                    .avgPopulationMin(avgMin)
                    .avgPopulationMax(avgMax)
                    .statHour(startHour)
                    .build();

            hourlyStatRepository.save(stat);
        }
        log.info("📊 [시간별 집계 배치] {} 시점의 1시간 단위 인구 평균 집계 완료", startHour);
    }

    // 60초마다 유효 수명이 만료된 1시간 이전의 과거 원시 데이터 오토 클리닝 배치
    @Scheduled(fixedDelay = 60000)
    public void cleanUpOldCongestionData() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        try {
            congestionRepository.deleteOlderThan(threshold);
            log.info("🧹 [데이터 청소 배치] {} 시점 이전의 만료된 과거 혼잡도 데이터 벌크 삭제 완료", threshold);
        } catch (Exception e) {
            log.error("❌ [데이터 청소 배치] 가동 중 에러 발생: {}", e.getMessage());
        }
    }

    // 서울시 실시간 인구데이터 등급 문표를 도메인 ENUM 코드로 정밀 매핑하는 헬퍼 메서드
    private CongestionLevel convertToDomainLevel(String level) {
        return switch (level) {
            case "여유" -> CongestionLevel.GREEN;
            case "보통" -> CongestionLevel.YELLOW;
            case "약간 혼잡" -> CongestionLevel.ORANGE;
            case "혼잡" -> CongestionLevel.RED;
            default -> CongestionLevel.YELLOW;
        };
    }
}