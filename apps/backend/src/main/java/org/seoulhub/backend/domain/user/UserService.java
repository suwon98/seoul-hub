package org.seoulhub.backend.domain.user;

import lombok.RequiredArgsConstructor;
import org.seoulhub.backend.global.config.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginSessionRepository sessions;
    private final JwtTokenProvider jwtTokenProvider;
    private final org.seoulhub.backend.global.config.RequestRateLimiter rateLimiter;
    private static final String DUMMY_HASH = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("unused-dummy-password");

    private final AccountLifecycleService accounts;

    public void signUp(UserSignUpRequestDto requestDto) {
        if (!rateLimiter.allow("signup-global", 40, 900000)) return;
        String email=requestDto.getEmail();
        if (!userRepository.existsByEmail(email)) {
            // No usable password until the mailbox owner completes verification.
            String unusable=passwordEncoder.encode(java.util.UUID.randomUUID().toString()+java.util.UUID.randomUUID());
            userRepository.saveAndFlush(User.builder().email(email).name("인증 대기").password(unusable).role(Role.USER).build());
        }
        accounts.requestVerification(email);
    }

    @Transactional
    public String login(UserLoginRequestDto requestDto) {
        String email = requestDto.getEmail();
        if (!rateLimiter.allow("login:"+email, 10, 900000)) throw new org.seoulhub.backend.global.error.TooManyRequestsException();
        User user = userRepository.findLockedByEmail(email).orElse(null);
        boolean valid = passwordEncoder.matches(requestDto.getPassword(), user == null ? DUMMY_HASH : user.getPassword());
        if (user == null || !valid) throw new org.springframework.security.authentication.BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        if (!user.isEmailVerified()) throw new org.springframework.security.authentication.DisabledException("이메일 인증을 먼저 완료해 주세요.");
        // The user row lock serializes successful concurrent logins.
        sessions.deleteByEmail(user.getEmail());
        sessions.flush();
        return jwtTokenProvider.createToken(user.getEmail(), user.getRole().name());
    }
}
