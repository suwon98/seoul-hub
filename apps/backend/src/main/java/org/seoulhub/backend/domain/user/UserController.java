package org.seoulhub.backend.domain.user;

import io.swagger.v3.oas.annotations.Operation;
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
    public ResponseEntity<Long> signUp(@RequestBody @Valid UserSignUpRequestDto requestDto) {
        Long userId = userService.signUp(requestDto);
        return ResponseEntity.ok(userId);
    }

    @Operation(summary = "이메일 중복 확인", description = "입력한 이메일이 이미 가입되어 있는지 실시간 검증합니다.")
    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmail(@RequestParam String email) {
        boolean isDuplicated = userService.isEmailDuplicated(email);
        return ResponseEntity.ok(isDuplicated);
    }
}
