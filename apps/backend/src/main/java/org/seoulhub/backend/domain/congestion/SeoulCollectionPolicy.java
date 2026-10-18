package org.seoulhub.backend.domain.congestion;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
@Component("seoulCollectionPolicy")
public class SeoulCollectionPolicy {
    private final SeoulApiBudgetRepository budgets;
    private final long dailyBudget;
    private final long requestDelayMs;
    public SeoulCollectionPolicy(SeoulApiBudgetRepository budgets,
            @Value("${seoul.collection.daily-budget:900}") long dailyBudget,
            @Value("${seoul.collection.request-delay-ms:0}") long minimumDelayMs) {
        if(dailyBudget<1 || dailyBudget>100000 || minimumDelayMs<0) throw new IllegalArgumentException("Invalid Seoul collection policy");
        this.budgets=budgets; this.dailyBudget=dailyBudget;
        requestDelayMs=Math.max(1000,minimumDelayMs);
    }
    public long getRequestDelayMs() { return requestDelayMs; }
    @Transactional(readOnly=true)
    public String nextAreaName() { return AreaCatalog.ALL.get(budgets.findById("population").map(SeoulApiBudget::getNextAreaIndex).orElse(0)).name(); }
    public long getDailyBudget() { return dailyBudget; }
    public record Status(long dailyBudget,long usedToday,long requestDelayMs,long cacheTtlSeconds) {}
    @Transactional(readOnly=true)
    public Status status() {
        var today=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        long used=budgets.findById("population").filter(b -> today.equals(b.getBudgetDate())).map(SeoulApiBudget::getUsed).orElse(0L);
        return new Status(dailyBudget,used,requestDelayMs,300);
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public synchronized boolean tryAcquire() {
        var budget=budgets.findLocked("population").orElseGet(() -> new SeoulApiBudget("population"));
        boolean allowed=budget.acquire(Instant.now(),dailyBudget,requestDelayMs);
        budgets.saveAndFlush(budget);
        return allowed;
    }
}
