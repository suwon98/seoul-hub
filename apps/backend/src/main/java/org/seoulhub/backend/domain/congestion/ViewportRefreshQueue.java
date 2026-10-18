package org.seoulhub.backend.domain.congestion;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
@Component
@RequiredArgsConstructor
public class ViewportRefreshQueue {
    private final CongestionService service;
    private final Map<String,Interest> interests=new HashMap<>();
    public record Request(@NotBlank @Pattern(regexp="[a-zA-Z0-9-]{1,64}") String clientId, @Min(0) long sequence,
        @Size(max=20) @NotNull List<@NotBlank String> visible, String selected) {}
    private record Interest(List<String> visible,String selected,Instant expires,long sequence) {}
    public synchronized void update(Request request) {
        // Resolve every supplied code before replacing the previous viewport.
        var visible=request.visible().stream().map(x -> AreaCatalog.resolve(x).name()).distinct().toList();
        String selected=request.selected()==null ? null : AreaCatalog.resolve(request.selected()).name();
        expire();
        if(interests.containsKey(request.clientId()) && interests.get(request.clientId()).sequence()>request.sequence()) return;
        if(!interests.containsKey(request.clientId()) && interests.size()>=128) throw new org.seoulhub.backend.global.error.TooManyRequestsException();
        interests.put(request.clientId(),new Interest(visible,selected,Instant.now().plusSeconds(45),request.sequence()));
    }
    private void expire() { interests.values().removeIf(x -> !x.expires().isAfter(Instant.now())); }
    public synchronized Optional<String> next() {
        expire();
        var selected=new TreeSet<String>(); var visible=new TreeSet<String>();
        for(var interest:interests.values()) { if(interest.selected()!=null) selected.add(interest.selected()); visible.addAll(interest.visible()); }
        var order=Comparator.comparing((String name) -> service.getCongestionByArea(name).getLastCheckedAt(),Comparator.nullsFirst(Comparator.naturalOrder())).thenComparing(Comparator.naturalOrder());
        var priority=selected.stream().filter(service::needsRefresh).min(order);
        return priority.isPresent() ? priority : visible.stream().filter(service::needsRefresh).min(order);
    }
}
