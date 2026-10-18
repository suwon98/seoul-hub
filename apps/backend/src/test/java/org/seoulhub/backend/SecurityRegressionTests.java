package org.seoulhub.backend;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
class SecurityRegressionTests extends AccountTestSupport {
    @Test void normalizedSignupAndDuplicateHaveSameResponse() throws Exception {
        String email=email(); signup(" "+email.toUpperCase()+" ");
        assertThat(users.findByEmail(email)).isPresent();
        assertThat(users.findByEmail(email).orElseThrow().isEmailVerified()).isFalse();
        activate(email);
        assertThat(users.findByEmail(email).orElseThrow().getPassword()).startsWith("$2").isNotEqualTo(password);
        signup(email);
        mvc.perform(get("/api/v1/users/check-email").param("email",email)).andExpect(status().isUnauthorized());
    }
    @Test void passwordAndNameBoundariesAreEnforcedAtVerification() throws Exception {
        String email=email();signup(email);String token=latestToken(email);
        for(String bad:new String[]{"short","a".repeat(65),"안전한비밀번호입니다123456"})
            mvc.perform(post("/api/v1/users/verify-email").contentType("application/json").content(body("token",token,"name","테스트","password",bad))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/users/verify-email").contentType("application/json").content(body("token",token,"name"," ","password",password))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/users/signup").contentType("application/json").content(body("email","bad"))).andExpect(status().isBadRequest());
        activate(email);
    }
    @Test void unknownUserAndWrongPasswordHaveSameResponse() throws Exception {
        String email=email();signup(email);activate(email);
        for(String candidate:new String[]{email,email()}) mvc.perform(post("/api/v1/users/login").contentType("application/json").content(body("email",candidate,"password","wrong-password")))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
    }
    @Test void logoutRevokesBearerImmediately() throws Exception {
        String email=email();signup(email);activate(email);String auth=login(email.toUpperCase());
        mvc.perform(get("/api/v1/users/me").header("Authorization",auth)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/users/logout").header("Authorization",auth)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/users/me").header("Authorization",auth)).andExpect(status().isUnauthorized());
    }
    @Test void bruteForceIsLimitedPerNormalizedAccount() throws Exception {
        String email=email();
        for(int i=0;i<10;i++) mvc.perform(post("/api/v1/users/login").contentType("application/json").content(body("email",email,"password","wrong-password"))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/users/login").contentType("application/json").content(body("email",email.toUpperCase(),"password","wrong-password"))).andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After","900"));
    }
    @Test void malformedJsonAndUnknownSearchAreClientErrors() throws Exception {
        mvc.perform(post("/api/v1/users/login").contentType("application/json").content("{")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/congestion/not-a-supported-area")).andExpect(status().isBadRequest());
    }
    @Test void corsRejectsOtherOriginsAndHealthHidesDetails() throws Exception {
        mvc.perform(options("/api/v1/users/login").header("Origin","https://evil.example").header("Access-Control-Request-Method","POST")).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.components").doesNotExist());
    }
}
