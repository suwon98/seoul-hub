package org.seoulhub.backend.domain.congestion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CongestionService {

    private final CongestionRepository congestionRepository;

    public CongestionResponseDto getCongestionByArea(String areaName) {
        Congestion congestion = congestionRepository.findTopByAreaNameOrderByUpdateTimeDesc(areaName)
                .orElseGet(() -> Congestion.builder()
                        .areaName(areaName)
                        .congestionLevel(CongestionLevel.RED)
                        .congestionMessage("사람이 매우 많아 이동 시 주변을 잘 살펴야 합니다.")
                        .populationMin(82000)
                        .populationMax(84000)
                        .updateTime(LocalDateTime.now())
                        .build());

        return new CongestionResponseDto(congestion);
    }
}
