package org.seoulhub.backend.domain.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.seoulhub.backend.global.mail.AccountMailSender;
import org.seoulhub.backend.global.config.RequestRateLimiter;
import org.seoulhub.backend.domain.question.QuestionRepository;
import org.seoulhub.backend.domain.amswer.AnswerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import java.security.*;
import java.time.*;
import java.util.*;
import static org.seoulhub.backend.domain.user.AccountToken.Purpose.*;

@Slf4j @Service @RequiredArgsConstructor @Transactional
public class AccountLifecycleService {
    private final UserRepository users;
    private final AccountTokenRepository tokens;
    private final LoginSessionRepository sessions;
    private final PasswordEncoder passwords;
    private final AccountMailSender mail;
    private final RequestRateLimiter limiter;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    @Value("${account.public-origin:http://localhost:3000}") private String publicOrigin;
    private static final SecureRandom RANDOM=new SecureRandom();

    public void requestVerification(String email) {
        if(!allowMail(email)) return;
        users.findLockedByEmail(email).filter(user -> !user.isEmailVerified()).ifPresent(user -> issue(user,VERIFY_EMAIL));
    }
    public void requestReset(String email) {
        if(!allowMail(email)) return;
        users.findLockedByEmail(email).filter(User::isEmailVerified).ifPresent(user -> issue(user,RESET_PASSWORD));
    }
    private boolean allowMail(String email) {
        // Identical public response for unknown accounts, throttling and provider errors.
        return limiter.allow("mail-account:"+hash(email),3,900000) && limiter.allow("mail-global",40,900000);
    }
    private void issue(User user, AccountToken.Purpose purpose) {
        byte[] entropy=new byte[32]; RANDOM.nextBytes(entropy);
        String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(entropy);
        int minutes=purpose==VERIFY_EMAIL ? 60 : 20;
        String path=purpose==VERIFY_EMAIL ? "/verify-email" : "/reset-password";
        String subject=purpose==VERIFY_EMAIL ? "[Seoul Hub] 이메일 인증" : "[Seoul Hub] 비밀번호 재설정";
        // Fragment stays out of HTTP access logs/referrers. GET never consumes the link.
        String body=subject+"\n\n아래 링크를 열고 직접 완료 버튼을 눌러 주세요. 링크는 "+minutes+"분 동안 한 번만 사용할 수 있습니다.\n"
            +publicOrigin+path+"#token="+raw+"\n\n요청하지 않았다면 이 메일을 무시하세요. 기존 비밀번호는 변경되지 않습니다.";
        try {
            mail.send(user.getEmail(),subject,body);
        } catch(RuntimeException e) {
            // No address, token, body, provider response or exception text in logs.
            log.warn("Account email delivery failed: purpose={}",purpose);
            return;
        }
        tokens.removePurpose(user.getId(),purpose);
        tokens.save(new AccountToken(hash(raw),user.getId(),purpose,Instant.now().plusSeconds(minutes*60L)));
    }
    public void verify(AccountRequests.VerifyRequest request) {
        User user=consume(request.token(),VERIFY_EMAIL);
        user.activate(request.name(),passwords.encode(request.password()));
        sessions.deleteByEmail(user.getEmail());
        tokens.removeAllForUser(user.getId());
    }
    public void reset(AccountRequests.ResetRequest request) {
        User user=consume(request.token(),RESET_PASSWORD);
        if(!user.isEmailVerified()) throw invalidLink();
        user.changePassword(passwords.encode(request.password()));
        sessions.deleteByEmail(user.getEmail());
        tokens.removeAllForUser(user.getId());
    }
    private User consume(String raw, AccountToken.Purpose purpose) {
        String hashed=hash(raw);
        Long id=tokens.findOwner(hashed,purpose).orElseThrow(AccountLifecycleService::invalidLink);
        User user=users.findLockedById(id).orElseThrow(AccountLifecycleService::invalidLink);
        // Atomic delete after the user lock: concurrent consumption can succeed only once.
        if(tokens.consume(hashed,id,purpose,Instant.now())!=1) throw invalidLink();
        return user;
    }
    private static IllegalArgumentException invalidLink() { return new IllegalArgumentException("유효하지 않거나 만료된 링크입니다. 새 메일을 요청해 주세요."); }
    public void deleteAccount(String email, AccountRequests.DeleteRequest request) {
        if(!limiter.allow("delete-account:"+hash(email),5,900000)) throw new org.seoulhub.backend.global.error.TooManyRequestsException();
        User user=users.findLockedByEmail(email).orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if(!passwords.matches(request.password(),user.getPassword())) throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        // Delete owned answers and replies to owned questions before deleting their parents.
        answers.deleteForAccount(user.getId());
        questions.deleteForAccount(user.getId());
        tokens.removeAllForUser(user.getId());
        sessions.deleteByEmail(email);
        users.delete(user);
    }
    @Scheduled(fixedDelay=3600000) @Transactional
    public void cleanExpiredTokens() { tokens.removeExpired(Instant.now()); }
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable"); }
    }
}
