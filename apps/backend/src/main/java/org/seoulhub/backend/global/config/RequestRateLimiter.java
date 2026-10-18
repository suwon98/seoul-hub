package org.seoulhub.backend.global.config;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
@Component
public class RequestRateLimiter {
    private record Window(long start,int count) {}
    private final Map<String,Window> windows=new HashMap<>();
    private final Clock clock;
    public RequestRateLimiter() { this(Clock.systemUTC()); }
    public RequestRateLimiter(Clock clock) { this.clock=clock; }
    public synchronized boolean allow(String key,int limit,long periodMillis) {
        long now=clock.millis();
        // Entries live at most 15 minutes; bounded memory, fail closed on exhaustion.
        windows.entrySet().removeIf(e -> now-e.getValue().start() >= 900000);
        Window w=windows.get(key);
        if(w==null || now-w.start()>=periodMillis) {
            if(windows.size()>=10000 && w==null) return false;
            windows.put(key,new Window(now,1));return true;
        }
        if(w.count()>=limit) return false;
        windows.put(key,new Window(w.start(),w.count()+1)); return true;
    }
}
