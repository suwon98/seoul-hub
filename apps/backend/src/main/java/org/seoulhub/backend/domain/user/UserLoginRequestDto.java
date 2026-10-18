package org.seoulhub.backend.domain.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserLoginRequestDto {

    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @jakarta.validation.constraints.Size(max=254)
    private String email;
    public void setEmail(String email) { this.email = email == null ? null : email.strip().toLowerCase(java.util.Locale.ROOT); }

    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    @jakarta.validation.constraints.Size(max=64)
    private String password;
}
