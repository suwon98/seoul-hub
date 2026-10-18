package org.seoulhub.backend.domain.congestion;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
public final class AreaCatalog {
    public record Area(String code,String name,String category,double lat,double lng) {}
    public static final List<Area> ALL;
    static {
        try(var in=AreaCatalog.class.getResourceAsStream("/seoul-areas.json")) {
            ALL=List.of(new ObjectMapper().readValue(in,Area[].class));
            if(ALL.isEmpty() || ALL.stream().map(Area::code).distinct().count()!=ALL.size()) throw new IllegalStateException("Invalid area catalog");
        } catch(Exception e) { throw new ExceptionInInitializerError(e); }
    }
    public static Area resolve(String input) {
        String name=switch(input) { case "홍대입구역" -> "홍대입구역(2호선)"; case "명동" -> "명동 관광특구"; case "이태원" -> "이태원 관광특구"; case "여의도" -> "여의도"; default -> input; };
        return ALL.stream().filter(a -> a.name().equals(name)||a.code().equals(input)).findFirst().orElseThrow(() -> new IllegalArgumentException("공식 거점 목록에서 지역을 선택해 주세요."));
    }
    public static List<String> namesFor(String input) {
        String name=resolve(input).name();
        return switch(name) { case "홍대입구역(2호선)" -> List.of(name,"홍대입구역"); case "명동 관광특구" -> List.of(name,"명동"); case "이태원 관광특구" -> List.of(name,"이태원"); default -> List.of(name); };
    }
    private AreaCatalog() {}
}
