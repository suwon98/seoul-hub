package org.seoulhub.backend.global.mail;
public interface AccountMailSender {
    void send(String recipient, String subject, String body);
}
