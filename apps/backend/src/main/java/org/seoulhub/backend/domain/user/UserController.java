package org.seoulhub.backend.domain.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.seoulhub.backend.global.config.JwtTokenProvider;
import java.security.Principal;
import java.util.Map;

@RestController @RequestMapping("/api/v1/users") @RequiredArgsConstructor
public class UserController {
    private final UserService users;
    private final AccountLifecycleService accounts;
    private final JwtTokenProvider tokens;
    private static final String RECEIPT="요청을 접수했습니다. 계정 상태와 발송 제한에 따라 안내 메일이 발송됩니다. 메일이 오지 않으면 스팸함을 확인하거나 잠시 후 다시 요청해 주세요.";

    @PostMapping("/signup")
    public ResponseEntity<Map<String,Object>> signUp(@RequestBody @Valid UserSignUpRequestDto request) {
        users.signUp(request);
        return receipt();
    }
    @PostMapping("/request-verification")
    public ResponseEntity<Map<String,Object>> requestVerification(@RequestBody @Valid AccountRequests.EmailRequest request) {
        accounts.requestVerification(request.email()); return receipt();
    }
    @PostMapping("/verify-email")
    public Map<String,Boolean> verify(@RequestBody @Valid AccountRequests.VerifyRequest request) {
        accounts.verify(request); return Map.of("success",true);
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String,Object>> forgot(@RequestBody @Valid AccountRequests.EmailRequest request) {
        accounts.requestReset(request.email()); return receipt();
    }
    @PostMapping("/reset-password")
    public Map<String,Boolean> reset(@RequestBody @Valid AccountRequests.ResetRequest request) {
        accounts.reset(request); return Map.of("success",true);
    }
    @PostMapping("/delete-account")
    public Map<String,Boolean> delete(Principal principal,@RequestBody @Valid AccountRequests.DeleteRequest request) {
        accounts.deleteAccount(principal.getName(),request); return Map.of("success",true);
    }
    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid UserLoginRequestDto request) { return new LoginResponse(true,users.login(request)); }
    @GetMapping("/me")
    public Map<String,String> me(Principal principal) { return Map.of("email",principal.getName()); }
    @PostMapping("/logout")
    public Map<String,Boolean> logout(@RequestHeader("Authorization") String auth) {
        tokens.revoke(auth.substring(7)); return Map.of("success",true);
    }
    private ResponseEntity<Map<String,Object>> receipt() { return ResponseEntity.accepted().body(Map.of("success",true,"message",RECEIPT)); }
    public record LoginResponse(boolean success,String accessToken) {}
}
