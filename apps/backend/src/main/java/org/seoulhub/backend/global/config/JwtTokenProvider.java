package org.seoulhub.backend.global.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.seoulhub.backend.domain.user.LoginSession;
import org.seoulhub.backend.domain.user.LoginSessionRepository;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final LoginSessionRepository sessions;
    private final org.seoulhub.backend.domain.user.UserRepository users;
    public JwtTokenProvider(@Value("${security.jwt.secret}") String secret, LoginSessionRepository sessions, org.seoulhub.backend.domain.user.UserRepository users) {
        secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.sessions = sessions;
        this.users = users;
    }
    @Transactional
    public String createToken(String email, String role) {
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(3600);
        String id = UUID.randomUUID().toString();
        sessions.save(new LoginSession(id, email, expires));
        return Jwts.builder().id(id).issuer("seoul-hub").subject(email).claim("role",role)
                .issuedAt(Date.from(now)).expiration(Date.from(expires)).signWith(secretKey).compact();
    }
    public Claims verifiedClaims(String token) {
        Claims claims = Jwts.parser().requireIssuer("seoul-hub").verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        if (claims.getId() == null || claims.getSubject() == null || claims.getExpiration() == null) throw new IllegalArgumentException("Invalid token");
        var session = sessions.findById(claims.getId()).orElseThrow(() -> new IllegalArgumentException("Session expired"));
        if (!session.getEmail().equals(claims.getSubject()) || !session.getExpiresAt().isAfter(Instant.now())) throw new IllegalArgumentException("Session expired");
        if (!users.findByEmail(claims.getSubject()).map(org.seoulhub.backend.domain.user.User::isEmailVerified).orElse(false)) throw new IllegalArgumentException("Account unavailable");
        return claims;
    }
    @Transactional
    public void revoke(String token) { sessions.deleteById(verifiedClaims(token).getId()); }
    @Scheduled(fixedDelay=3600000)
    @Transactional
    public void removeExpiredSessions() { sessions.deleteByExpiresAtBefore(Instant.now()); }
}
