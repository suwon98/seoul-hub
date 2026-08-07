package org.seoulhub.backend.domain.congestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CongestionService {

    private final CongestionRepository congestionRepository;
    private final CongestionHourlyStatRepository hourlyStatRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClient webClient = WebClient.builder()
            .baseUrl("http://openapi.seoul.go.kr:8088")
            .build();

    @Value("${seoul.api.key}")
    private String apiKey;

    public List<CongestionResponseDto> getAllLatestCongestion() {
        return congestionRepository.findAllLatestCongestion().stream()
                .map(CongestionResponseDto::new)
                .toList();
    }

    @Transactional
    public CongestionResponseDto getCongestionByArea(String areaName) {
        return congestionRepository.findTopByAreaNameOrderByUpdateTimeDesc(areaName)
                .map(CongestionResponseDto::new)
                .orElseGet(() -> fetchAndSaveFromSeoulApi(areaName));
    }

    public List<CongestionResponseDto> getCongestionHistory(String areaName) {
        List<CongestionHourlyStat> stats = hourlyStatRepository.findTop24ByAreaNameOrderByStatHourAsc(areaName);

        if (stats.isEmpty()) {
            return congestionRepository.findTop20ByAreaNameOrderByUpdateTimeAsc(areaName).stream()
                    .map(CongestionResponseDto::new)
                    .toList();
        }

        return stats.stream()
                .map(CongestionResponseDto::new)
                .toList();
    }

    private CongestionResponseDto fetchAndSaveFromSeoulApi(String areaName) {
        try {
            String responseBody = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{key}/json/citydata_ppltn/1/1/{area}")
                            .build(apiKey, areaName))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode arrayNode = root.path("SeoulRtd.citydata_ppltn");
            JsonNode rowNode = (arrayNode.isMissingNode() || !arrayNode.isArray()) ? null : arrayNode.get(0);

            if (rowNode == null) {
                return createFallbackDto(areaName);
            }

            String apiLevel = rowNode.path("AREA_CONGEST_LVL").asText("보통");
            String apiMessage = rowNode.path("AREA_CONGEST_MSG").asText("");
            int popMin = rowNode.path("AREA_PPLTN_MIN").asInt(0);
            int popMax = rowNode.path("AREA_PPLTN_MAX").asInt(0);

            CongestionLevel level = convertToDomainLevel(apiLevel);

            Congestion congestion = Congestion.builder()
                    .areaName(areaName)
                    .congestionLevel(level)
                    .congestionMessage(apiMessage)
                    .populationMin(popMin)
                    .populationMax(popMax)
                    .updateTime(LocalDateTime.now())
                    .build();

            Congestion saved = congestionRepository.save(congestion);
            return new CongestionResponseDto(saved);
        } catch (Exception e) {
            return createFallbackDto(areaName);
        }
    }

    private CongestionResponseDto createFallbackDto(String areaName) {
        Congestion fallback = Congestion.builder()
                .areaName(areaName)
                .congestionLevel(CongestionLevel.UNKNOWN)
                .congestionMessage("공공 API 장애로 인해 실시간 데이터를 불러올 수 없습니다.")
                .populationMin(0)
                .populationMax(0)
                .updateTime(LocalDateTime.now())
                .build();
        return new CongestionResponseDto(fallback);
    }

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