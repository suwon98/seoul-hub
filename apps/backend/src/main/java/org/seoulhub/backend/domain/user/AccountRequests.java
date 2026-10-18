package org.seoulhub.backend.domain.user;
import jakarta.validation.constraints.*;
public final class AccountRequests {
    private AccountRequests() {}
    public record EmailRequest(@NotBlank @Email @Size(max=254) String email) {
        public EmailRequest { if(email!=null) email=email.strip().toLowerCase(java.util.Locale.ROOT); }
    }
    public record VerifyRequest(
        @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{43}") String token,
        @NotBlank @Size(min=2,max=40) String name,
        @NotBlank @Pattern(regexp="^[\\x20-\\x7E]{12,64}$",message="비밀번호는 영문, 숫자, 공백, 기호 12~64자로 입력해 주세요.") String password) {
        public VerifyRequest { if(name!=null) name=name.strip(); }
    }
    public record ResetRequest(
        @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{43}") String token,
        @NotBlank @Pattern(regexp="^[\\x20-\\x7E]{12,64}$",message="비밀번호는 영문, 숫자, 공백, 기호 12~64자로 입력해 주세요.") String password) {}
    public record DeleteRequest(@NotBlank @Size(max=64) String password,
        @NotBlank @Pattern(regexp="회원 탈퇴",message="회원 탈퇴를 정확히 입력해 주세요.") String confirmation) {}
}
