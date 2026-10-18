package org.seoulhub.backend.domain.congestion;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class ViewportRefreshTests {
    private CongestionResponseDto dto(String name) {
        return new CongestionResponseDto(Congestion.builder().areaName(name).congestionLevel(CongestionLevel.GREEN).populationMin(1).populationMax(2).updateTime(LocalDateTime.now()).build());
    }
    @Test void selectionWinsAndNewViewportReplacesOldEvenWithLateRequests() {
        var service=mock(CongestionService.class);
        when(service.needsRefresh(anyString())).thenReturn(true);
        when(service.getCongestionByArea(anyString())).thenAnswer(x -> dto(x.getArgument(0)));
        var queue=new ViewportRefreshQueue(service);
        queue.update(new ViewportRefreshQueue.Request("client",1,List.of("POI001"),"POI013"));
        assertThat(queue.next()).contains(AreaCatalog.resolve("POI013").name());
        queue.update(new ViewportRefreshQueue.Request("client",3,List.of("POI002"),null));
        queue.update(new ViewportRefreshQueue.Request("client",2,List.of("POI001"),"POI013"));
        assertThat(queue.next()).contains(AreaCatalog.resolve("POI002").name());
        queue.update(new ViewportRefreshQueue.Request("client",4,List.of(),null));
        assertThat(queue.next()).isEmpty();
    }
    @Test void cachedAreaIsSharedAcrossClientsAndNotScheduled() {
        var service=mock(CongestionService.class);
        var queue=new ViewportRefreshQueue(service);
        queue.update(new ViewportRefreshQueue.Request("one",1,List.of("POI001"),"POI001"));
        queue.update(new ViewportRefreshQueue.Request("two",1,List.of("POI001"),"POI001"));
        assertThat(queue.next()).isEmpty();
        when(service.needsRefresh(anyString())).thenReturn(true);
        when(service.getCongestionByArea(anyString())).thenAnswer(x -> dto(x.getArgument(0)));
        assertThat(queue.next()).contains(AreaCatalog.resolve("POI001").name());
        when(service.needsRefresh(anyString())).thenReturn(false);
        assertThat(queue.next()).isEmpty();
    }
    @Test void recentlyCheckedOldObservationDoesNotSpendBudgetAgain() {
        var records=mock(CongestionRepository.class);
        var latest=mock(LatestObservationRepository.class);
        var policy=mock(SeoulCollectionPolicy.class);
        var row=new LatestObservation(AreaCatalog.resolve("강남역"),Congestion.builder().areaName("강남역").congestionLevel(CongestionLevel.GREEN).populationMin(1).populationMax(2).updateTime(LocalDateTime.now().minusHours(1)).build());
        row.checked(Instant.now());
        when(latest.findById(anyString())).thenReturn(Optional.of(row));
        var service=new CongestionService(records,mock(CongestionHourlyStatRepository.class),latest,policy);
        assertThat(service.needsRefresh("강남역")).isFalse();
        assertThat(service.collect("강남역").getPopulationMax()).isEqualTo(2);
        verifyNoInteractions(policy);
        row.checked(Instant.now().minusSeconds(301));
        assertThat(service.needsRefresh("강남역")).isTrue();
    }
}
