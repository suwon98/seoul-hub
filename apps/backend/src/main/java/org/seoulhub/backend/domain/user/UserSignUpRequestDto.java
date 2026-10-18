package org.seoulhub.backend.domain.user;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Getter @NoArgsConstructor
public class UserSignUpRequestDto {
    @NotBlank @Email @Size(max=254) private String email;
    public void setEmail(String email) { this.email=email==null ? null : email.strip().toLowerCase(java.util.Locale.ROOT); }
}
