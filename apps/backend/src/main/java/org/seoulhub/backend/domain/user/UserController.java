package org.seoulhub.backend.domain.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "user", description = "회원 인증 및 계정 관리 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "일반 이메일 회원가입", description = "새로운 회원을 등록합니다. 비밀번호는 대소문자, 숫자, 특수문자가 강제됩니다.")
    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(@RequestBody @Valid UserSignUpRequestDto requestDto) {
        Long userId = userService.signUp(requestDto);
        return ResponseEntity.ok(new SignUpResponse(true, userId));
    }

    @Operation(summary = "이메일 중복 확인", description = "입력한 이메일이 이미 가입되어 있는지 실시간 검증합니다.")
    @GetMapping("/check-email")
    public ResponseEntity<EmailCheckResponse> checkEmail(@RequestParam String email) {
        boolean isDuplicated = userService.isEmailDuplicated(email);

        return ResponseEntity.ok(new EmailCheckResponse(isDuplicated));
    }

    @Schema(description = "회원가입 최종 응답 객체")
    public record SignUpResponse(boolean success, Long id) {}

    @Schema(description = "이메일 중복 체크 응답 객체")
    public record EmailCheckResponse(
            @Schema(description = "중복 여부", example = "false")
            boolean isDuplicated
    ) {}

    @Operation(summary = "정규 회원 로그인", description = "이메일과 비밀번호를 검증하여 1시간 수명의 정식 JWT 인증 토큰을 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @jakarta.validation.Valid UserLoginRequestDto requestDto) {
        String token = userService.login(requestDto);
        return ResponseEntity.ok(new LoginResponse(true, token));
    }

    @Schema(description = "로그인 최종 인증 응답 객체")
    public record LoginResponse(
            @Schema(description = "성공 여부")
            boolean success,
            @Schema(description = "Bearer Access Token 문자열")
            String accessToken
    ) {}
}