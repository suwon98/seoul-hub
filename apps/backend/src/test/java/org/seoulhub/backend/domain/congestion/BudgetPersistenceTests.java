package org.seoulhub.backend.domain.congestion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest
class BudgetPersistenceTests {
    @Autowired SeoulCollectionPolicy policy;
    @Autowired SeoulApiBudgetRepository budgets;
    @Test void newPolicyInstanceCannotBypassPersistedIntervalOrRestartCursor() {
        budgets.deleteAll();
        try {
            assertThat(policy.nextAreaName()).isEqualTo(AreaCatalog.ALL.getFirst().name());
            assertThat(policy.tryAcquire()).isTrue();
            var restarted=new SeoulCollectionPolicy(budgets,900,0);
            assertThat(restarted.status().usedToday()).isEqualTo(1);
            assertThat(restarted.nextAreaName()).isEqualTo(AreaCatalog.ALL.get(1).name());
            // Persisted entity enforces the interval even when process memory is gone.
            assertThat(budgets.findById("population").orElseThrow().acquire(java.time.Instant.now(),900,96000)).isFalse();
            assertThat(policy.tryAcquire()).isFalse();
            assertThat(policy.status().usedToday()).isEqualTo(1);
        } finally { budgets.deleteAll(); }
    }
}
