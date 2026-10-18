package org.seoulhub.backend.domain.congestion;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
class SeoulCollectionTests {
    @Test void canonicalAreaNameObservationTimestampAndDeduplication() throws Exception {
        var path=new AtomicReference<String>();
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            byte[] body="{\"SeoulRtd.citydata_ppltn\":[{\"AREA_CONGEST_LVL\":\"보통\",\"AREA_CONGEST_MSG\":\"관측값\",\"AREA_PPLTN_MIN\":\"100\",\"AREA_PPLTN_MAX\":\"200\",\"PPLTN_TIME\":\"2026-10-15 20:50\"}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();
        });
        server.start();
        try {
            var repo=mock(CongestionRepository.class);var stats=mock(CongestionHourlyStatRepository.class);
            when(repo.findTopByAreaNameOrderByUpdateTimeDesc(anyString())).thenReturn(Optional.empty());
            when(repo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            var latest=mock(LatestObservationRepository.class);
            when(latest.findById(anyString())).thenReturn(Optional.empty());
            var policy=mock(SeoulCollectionPolicy.class);
            when(policy.tryAcquire()).thenReturn(true);
            var service=new CongestionService(repo,stats,latest,policy);
            ReflectionTestUtils.setField(service,"apiKey","fixture-key");
            ReflectionTestUtils.setField(service,"webClient",WebClient.builder().baseUrl("http://127.0.0.1:"+server.getAddress().getPort()).build());
            var result=service.collect("홍대입구역");
            assertThat(path.get()).endsWith("/"+AreaCatalog.resolve("홍대입구역").code());
            assertThat(result.getPopulationMax()).isEqualTo(200);
            assertThat(result.getUpdateTime()).isEqualTo(LocalDateTime.of(2026,10,15,20,50));
            service.collect("홍대입구역");
            verify(repo,times(1)).save(any());
        } finally { server.stop(0); }
    }
}
