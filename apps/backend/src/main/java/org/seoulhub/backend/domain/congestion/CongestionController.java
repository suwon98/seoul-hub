package org.seoulhub.backend.domain.congestion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "congestion", description = "실시간 인구 혼잡도 API")
@RestController
@RequestMapping("/api/v1/congestion")
@RequiredArgsConstructor
public class CongestionController {

    private final CongestionService congestionService;

    @Operation(summary = "지역별 실시간 인구 혼잡도 조회", description = "지정한 서울시 주요 지역의 실시간 인구 수 및 혼잡도 단계를 조회합니다.")
    @GetMapping
    public ResponseEntity<CongestionResponseDto> getCongestion(@RequestParam(defaultValue = "강남역") String areaName) {
        CongestionResponseDto response = congestionService.getCongestionByArea(areaName);
        return ResponseEntity.ok(response);
    }
}
