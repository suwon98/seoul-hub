package org.seoulhub.backend.domain.congestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.transaction.support.TransactionTemplate;
@Component @RequiredArgsConstructor
public class LatestObservationBackfill implements SmartInitializingSingleton {
    private final CongestionRepository history;
    private final LatestObservationRepository latest;
    private final TransactionTemplate transactions;
    @Override public void afterSingletonsInstantiated() {
        transactions.executeWithoutResult(status -> {
            for(var old:history.findAllLatestCongestion()) {
                AreaCatalog.Area area;
                try { area=AreaCatalog.resolve(old.getAreaName()); } catch(IllegalArgumentException ignored) { continue; }
                var observation=Congestion.builder().areaName(area.name()).congestionLevel(old.getCongestionLevel()).congestionMessage(old.getCongestionMessage()).populationMin(old.getPopulationMin()).populationMax(old.getPopulationMax()).updateTime(old.getUpdateTime()).collectedAt(old.getCollectedAt()).build();
                var row=latest.findById(area.code()).orElseGet(() -> new LatestObservation(area,observation));
                row.apply(observation); latest.save(row);
            }
        });
    }
}
