package org.seoulhub.backend;
import org.junit.jupiter.api.Test;
import org.seoulhub.backend.global.mail.BrevoAccountMailSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Flow;
import java.util.concurrent.CompletableFuture;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class BrevoMailTests {
    @SuppressWarnings("unchecked")
    @Test void usesHttpsApiWithCorrectPrivatePayloadAndBoundedTimeout() throws Exception {
        HttpClient client=mock(HttpClient.class);
        HttpResponse<Void> response=mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(201);
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenAnswer(invocation -> {
            HttpRequest request=invocation.getArgument(0);
            assertThat(request.uri().toString()).isEqualTo("https://api.brevo.com/v3/smtp/email");
            assertThat(request.method()).isEqualTo("POST");
            assertThat(request.headers().firstValue("api-key")).contains("test-provider-key");
            assertThat(request.timeout().orElseThrow().toSeconds()).isEqualTo(8);
            var bytes=new ByteArrayOutputStream();var complete=new CompletableFuture<Void>();
            request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
                public void onSubscribe(Flow.Subscription subscription) {subscription.request(Long.MAX_VALUE);}
                public void onNext(ByteBuffer buffer) {byte[] chunk=new byte[buffer.remaining()];buffer.get(chunk);bytes.writeBytes(chunk);}
                public void onError(Throwable error) {complete.completeExceptionally(error);}
                public void onComplete() {complete.complete(null);}
            });
            complete.get();
            var payload=new ObjectMapper().readTree(bytes.toByteArray());
            assertThat(payload.path("sender").path("email").asText()).isEqualTo("sender@example.com");
            assertThat(payload.path("to").get(0).path("email").asText()).isEqualTo("owner@example.com");
            assertThat(payload.path("textContent").asText()).isEqualTo("private link body");
            return response;
        });
        new BrevoAccountMailSender(client,"test-provider-key","sender@example.com","Seoul Hub").send("owner@example.com","인증","private link body");
        verify(client).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
    }
    @SuppressWarnings("unchecked")
    @Test void providerErrorsNeverExposeSecretOrResponse() throws Exception {
        HttpClient client=mock(HttpClient.class);
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenThrow(new java.io.IOException("secret response with recipient"));
        assertThatThrownBy(() -> new BrevoAccountMailSender(client,"test-provider-key","sender@example.com","Seoul Hub").send("owner@example.com","인증","private link"))
            .hasMessage("Mail delivery unavailable").hasNoCause();
    }
}
