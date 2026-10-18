package org.seoulhub.backend.domain.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity @Table(name="account_tokens", indexes=@Index(name="idx_account_token_expiry", columnList="expiresAt"))
@Getter @NoArgsConstructor
public class AccountToken {
    public enum Purpose { VERIFY_EMAIL, RESET_PASSWORD }
    @Id @Column(length=64) private String tokenHash;
    @Column(nullable=false) private Long userId;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=32) private Purpose purpose;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP)
    @Column(nullable=false) private Instant expiresAt;
    public AccountToken(String hash, Long userId, Purpose purpose, Instant expiresAt) {
        this.tokenHash=hash; this.userId=userId; this.purpose=purpose; this.expiresAt=expiresAt;
    }
}
