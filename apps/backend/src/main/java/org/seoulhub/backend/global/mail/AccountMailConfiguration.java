package org.seoulhub.backend.global.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

@Configuration
public class AccountMailConfiguration {
    @Bean
    AccountMailSender accountMailSender(Environment env) {
        String mode=env.getProperty("account.mail.mode", "disabled");
        boolean local=Arrays.asList(env.getActiveProfiles()).contains("local");
        boolean prod=Arrays.asList(env.getActiveProfiles()).contains("prod");
        if("file".equals(mode)) {
            if(!local || prod) throw new IllegalStateException("File mailbox is available only in local profile");
            Path directory=Path.of("data", "mail");
            return (recipient, subject, body) -> {
                try {
                    Files.createDirectories(directory);
                    // Never serve this directory over HTTP; local developer mailbox only.
                    Files.writeString(directory.resolve(UUID.randomUUID()+".json"), new ObjectMapper().writeValueAsString(
                        Map.of("to",recipient,"subject",subject,"body",body,"createdAt",Instant.now().toString())), StandardOpenOption.CREATE_NEW);
                } catch(Exception e) { throw new IllegalStateException("Local mailbox unavailable"); }
            };
        }
        if("brevo".equals(mode)) {
            String key=env.getProperty("account.mail.api-key", "");
            String sender=env.getProperty("account.mail.sender-email", "");
            String senderName=env.getProperty("account.mail.sender-name", "Seoul Hub");
            if(key.isBlank() || !sender.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalStateException("Mail API key and verified sender are required");
            HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
            return new BrevoAccountMailSender(client,key,sender,senderName);
        }
        if(prod) throw new IllegalStateException("Production requires MAIL_MODE=brevo");
        return (recipient,subject,body) -> { throw new IllegalStateException("Mail is not configured"); };
    }
}
