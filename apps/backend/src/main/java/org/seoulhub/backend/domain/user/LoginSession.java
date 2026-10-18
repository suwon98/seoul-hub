package org.seoulhub.backend.domain.user;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
@Entity
@Table(name="login_sessions", indexes=@Index(name="idx_session_expiry", columnList="expiresAt"))
@Getter @NoArgsConstructor
public class LoginSession {
    @Id @Column(length=36) private String id;
    @Column(nullable=false) private String email;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP)
    @Column(nullable=false) private Instant expiresAt;
    public LoginSession(String id, String email, Instant expiresAt) { this.id=id; this.email=email; this.expiresAt=expiresAt; }
}
