package org.seoulhub.backend.domain.congestion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "congestion", description = "실시간 인구 혼잡도 API")
@RestController
@RequestMapping("/api/v1/congestion")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class CongestionController {

    private final CongestionService congestionService;

    @Operation(summary = "전체 거점 실시간 인구 혼잡도 목록 조회", description = "수집된 모든 거점의 최신 실시간 인구 혼잡도 목록을 조회합니다.")
    @GetMapping("/all")
    public ResponseEntity<List<CongestionResponseDto>> getAllCongestion() {
        List<CongestionResponseDto> response = congestionService.getAllLatestCongestion();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "지역별 실시간 인구 혼잡도 조회", description = "지정한 서울시 주요 지역의 실시간 인구 수 및 혼잡도 단계를 조회합니다.")
    @GetMapping("/{areaName}")
    public ResponseEntity<CongestionResponseDto> getCongestion(@PathVariable String areaName) {
        CongestionResponseDto response = congestionService.getCongestionByArea(areaName);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "지역별 실시간 인구 혼잡도 히스토리 조회", description = "지정한 서울시 주요 지역의 수집된 과거 인구 혼잡도 추이 기록을 조회합니다.")
    @GetMapping("/{areaName}/history")
    public ResponseEntity<List<CongestionResponseDto>> getCongestionHistory(@PathVariable String areaName) {
        List<CongestionResponseDto> response = congestionService.getCongestionHistory(areaName);
        return ResponseEntity.ok(response);
    }
}