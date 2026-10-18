package org.seoulhub.backend.global.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

public final class BrevoAccountMailSender implements AccountMailSender {
    private final HttpClient client;
    private final String key;
    private final String sender;
    private final String senderName;
    public BrevoAccountMailSender(HttpClient client,String key,String sender,String senderName) {
        this.client=client;this.key=key;this.sender=sender;this.senderName=senderName;
    }
    @Override public void send(String recipient,String subject,String body) {
        try {
            String payload=new ObjectMapper().writeValueAsString(Map.of("sender",Map.of("email",sender,"name",senderName),
                "to",List.of(Map.of("email",recipient)),"subject",subject,"textContent",body));
            HttpRequest request=HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email"))
                .timeout(Duration.ofSeconds(8)).header("api-key",key).header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload)).build();
            var response=client.send(request,HttpResponse.BodyHandlers.discarding());
            if(response.statusCode()<200 || response.statusCode()>=300) throw new IllegalStateException("Mail provider rejected request");
        } catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Mail delivery interrupted"); }
        catch(Exception e) { throw new IllegalStateException("Mail delivery unavailable"); }
    }
}
