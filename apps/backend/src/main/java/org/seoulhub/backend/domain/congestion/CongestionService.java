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
    private final LatestObservationRepository latestRepository;
    private final SeoulCollectionPolicy collectionPolicy;
    public static final List<String> AREAS = AreaCatalog.ALL.stream().map(AreaCatalog.Area::name).toList();
    private final java.util.Map<String, Long> lastAttempt = new java.util.HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClient webClient = WebClient.builder()
            .baseUrl("http://openapi.seoul.go.kr:8088")
            .build();

    @Value("${seoul.api.key}")
    private String apiKey;

    public List<CongestionResponseDto> getAllLatestCongestion() {
        var latest = latestRepository.findAll().stream().collect(java.util.stream.Collectors.toMap(LatestObservation::getAreaCode, x -> x));
        return AreaCatalog.ALL.stream().map(a -> latest.containsKey(a.code()) ? new CongestionResponseDto(latest.get(a.code())) : createFallbackDto(a.name())).toList();
    }

    public CongestionResponseDto getCongestionByArea(String areaName) {
        return latestOrFallback(AreaCatalog.resolve(areaName).name());
    }

    private void validateArea(String area) {
        AreaCatalog.resolve(area);
    }

    public List<CongestionResponseDto> getCongestionHistory(String areaName) {
        areaName=AreaCatalog.resolve(areaName).name();
        List<CongestionHourlyStat> stats = hourlyStatRepository.findTop24ByAreaNameInOrderByStatHourDesc(AreaCatalog.namesFor(areaName));

        if (stats.isEmpty()) {
            return congestionRepository.findTop20ByAreaNameInOrderByUpdateTimeDesc(AreaCatalog.namesFor(areaName)).stream()
                    .map(CongestionResponseDto::new)
                    .sorted(java.util.Comparator.comparing(CongestionResponseDto::getUpdateTime))
                    .toList();
        }

        return stats.stream().sorted(java.util.Comparator.comparing(CongestionHourlyStat::getStatHour))
                .map(CongestionResponseDto::new)
                .toList();
    }

    public synchronized boolean needsRefresh(String name) {
        var area=AreaCatalog.resolve(name);
        if(System.currentTimeMillis()-lastAttempt.getOrDefault(area.name(),0L)<60000) return false;
        return latestRepository.findById(area.code()).map(row -> row.getLastCheckedAt()==null || row.getLastCheckedAt().isBefore(java.time.Instant.now().minusSeconds(300))).orElse(true);
    }

    @Transactional
    public synchronized CongestionResponseDto collect(String areaName) {
        areaName=AreaCatalog.resolve(areaName).name();
        if (!needsRefresh(areaName)) return latestOrFallback(areaName);
        long now = System.currentTimeMillis();
        if (now - lastAttempt.getOrDefault(areaName, 0L) < 60000) return latestOrFallback(areaName);
        if (apiKey.isBlank() || !collectionPolicy.tryAcquire()) return latestOrFallback(areaName);
        lastAttempt.put(areaName, now);
        final String areaCode=AreaCatalog.resolve(areaName).code();
        try {
            String responseBody = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{key}/json/citydata_ppltn/1/1/{area}")
                            .build(apiKey, areaCode))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(java.time.Duration.ofSeconds(8));

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode arrayNode = root.path("SeoulRtd.citydata_ppltn");
            JsonNode rowNode = (arrayNode.isMissingNode() || !arrayNode.isArray()) ? null : arrayNode.get(0);

            if (rowNode == null) {
                return latestOrFallback(areaName);
            }

            String apiLevel = rowNode.path("AREA_CONGEST_LVL").asText();
            if (!rowNode.path("AREA_PPLTN_MIN").asText().matches("[0-9]+") || !rowNode.path("AREA_PPLTN_MAX").asText().matches("[0-9]+")) return latestOrFallback(areaName);
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
                    .updateTime(LocalDateTime.parse(rowNode.path("PPLTN_TIME").asText(), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .build();

            if (congestion.getPopulationMin() < 0 || congestion.getPopulationMax() < congestion.getPopulationMin()) return latestOrFallback(areaName);
            var area=AreaCatalog.resolve(areaName);
            var previous=latestRepository.findById(area.code());
            if(congestion.getUpdateTime().isAfter(LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul")).plusMinutes(5))) return latestOrFallback(areaName);
            // A valid repeat response confirms API reachability without inventing a new observation.
            if (congestionRepository.existsByAreaNameAndUpdateTime(areaName, congestion.getUpdateTime())) {
                previous.ifPresent(row -> { row.checked(java.time.Instant.now()); latestRepository.save(row); });
                return latestOrFallback(areaName);
            }
            Congestion saved = congestionRepository.save(congestion);
            var latest=previous.orElseGet(() -> new LatestObservation(area,saved));
            latest.apply(saved); latest.checked(java.time.Instant.now()); latestRepository.save(latest);
            log.info("Seoul observation stored: area={}, observedAt={}, level={}, population={}-{}", areaName, saved.getUpdateTime(), saved.getCongestionLevel(), saved.getPopulationMin(), saved.getPopulationMax());
            return new CongestionResponseDto(latest);
        } catch (Exception e) {
            // WebClient exceptions include the API-key URL. Never log the exception or raw response.
            log.warn("Seoul collection unavailable: area={}, errorType={}", areaName, e.getClass().getSimpleName());
            return latestOrFallback(areaName);
        }
    }

    private CongestionResponseDto latestOrFallback(String areaName) {
        return latestRepository.findById(AreaCatalog.resolve(areaName).code()).map(x -> new CongestionResponseDto(x)).orElseGet(() -> createFallbackDto(areaName));
    }

    private CongestionResponseDto createFallbackDto(String areaName) {
        Congestion fallback = Congestion.builder()
                .areaName(areaName)
                .congestionLevel(CongestionLevel.UNKNOWN)
                .congestionMessage("최신 관측 데이터가 없습니다. 잠시 후 다시 확인해 주세요.")
                .populationMin(0)
                .populationMax(0)
                .updateTime(null)
                .build();
        return new CongestionResponseDto(fallback);
    }

    private CongestionLevel convertToDomainLevel(String level) {
        return switch (level) {
            case "여유" -> CongestionLevel.GREEN;
            case "보통" -> CongestionLevel.YELLOW;
            case "약간 혼잡", "약간 붐빔" -> CongestionLevel.ORANGE;
            case "혼잡", "붐빔" -> CongestionLevel.RED;
            default -> CongestionLevel.UNKNOWN;
        };
    }
}