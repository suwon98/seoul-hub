package org.seoulhub.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.seoulhub.backend.domain.user.*;
import org.seoulhub.backend.domain.question.*;
import org.seoulhub.backend.domain.amswer.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountLifecycleTests extends AccountTestSupport {
    @Autowired AccountLifecycleService accounts;
    @Autowired PasswordEncoder encoder;
    @Autowired QuestionRepository questions;
    @Autowired AnswerRepository answers;

    @Test void onlyMailboxOwnerCanSetCredentialsAndVerifyOnce() throws Exception {
        String email=email();signup(email);String token=latestToken(email);
        var stored=tokens.findById(AccountLifecycleService.hash(token)).orElseThrow();
        assertThat(stored.getTokenHash()).doesNotContain(token).hasSize(64);
        assertThat(stored.getExpiresAt()).isAfter(Instant.now()).isBefore(Instant.now().plusSeconds(3601));
        mvc.perform(post("/api/v1/users/login").contentType("application/json").content(body("email",email,"password",password))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/verify-email").param("token",token)).andExpect(status().isMethodNotAllowed());
        activate(email);login(email);
        mvc.perform(post("/api/v1/users/verify-email").contentType("application/json").content(body("token",token,"name","탈취시도","password",password))).andExpect(status().isBadRequest());
    }
    @Test void legacyAccountAndOldSessionCannotBypassEmailVerification() throws Exception {
        String email=email();
        users.save(User.builder().email(email).name("기존회원").password(encoder.encode(password)).role(Role.USER).build());
        mvc.perform(post("/api/v1/users/login").contentType("application/json").content(body("email",email,"password",password))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/users/request-verification").contentType("application/json").content(body("email",email))).andExpect(status().isAccepted());
        activate(email);login(email);
    }
    @Test void expiredAndWrongPurposeLinksAreRejected() throws Exception {
        String email=email();signup(email);String token=latestToken(email);
        mvc.perform(post("/api/v1/users/reset-password").contentType("application/json").content(body("token",token,"password",password))).andExpect(status().isBadRequest());
        var stored=tokens.findById(AccountLifecycleService.hash(token)).orElseThrow();
        tokens.save(new AccountToken(stored.getTokenHash(),stored.getUserId(),stored.getPurpose(),Instant.now().minusSeconds(1)));
        mvc.perform(post("/api/v1/users/verify-email").contentType("application/json").content(body("token",token,"name","만료검증","password",password))).andExpect(status().isBadRequest());
        assertThat(users.findByEmail(email).orElseThrow().isEmailVerified()).isFalse();
    }
    @Test void resendingInvalidatesOldLinkAndDoesNotSendUnlimitedMail() throws Exception {
        String email=email();signup(email);String old=latestToken(email);
        for(int i=0;i<4;i++) mvc.perform(post("/api/v1/users/request-verification").contentType("application/json").content(body("email",email))).andExpect(status().isAccepted());
        verify(mail,times(3)).send(eq(email),anyString(),anyString());
        mvc.perform(post("/api/v1/users/verify-email").contentType("application/json").content(body("token",old,"name","테스트","password",password))).andExpect(status().isBadRequest());
        activate(email);
    }
    @Test void unknownAndKnownResetRequestsHaveSamePublicResponse() throws Exception {
        String email=email();signup(email);activate(email);
        var known=mvc.perform(post("/api/v1/users/forgot-password").contentType("application/json").content(body("email",email))).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        String unknown=email();
        var absent=mvc.perform(post("/api/v1/users/forgot-password").contentType("application/json").content(body("email",unknown))).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        assertThat(known).isEqualTo(absent);
        verify(mail,never()).send(eq(unknown),anyString(),anyString());
    }
    @Test void failedDeliveryKeepsExistingLinkAndDoesNotClaimDelivery() throws Exception {
        String email=email();signup(email);String valid=latestToken(email);
        doThrow(new IllegalStateException("provider sensitive response")).when(mail).send(eq(email),anyString(),anyString());
        mvc.perform(post("/api/v1/users/request-verification").contentType("application/json").content(body("email",email))).andExpect(status().isAccepted()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("계정 상태와 발송 제한")));
        assertThat(tokens.existsById(AccountLifecycleService.hash(valid))).isTrue();
        activate(email);
    }
    @Test void resetChangesPasswordRevokesAllSessionsAndIsSingleUse() throws Exception {
        String email=email();signup(email);activate(email);String first=login(email),second=login(email);
        mvc.perform(post("/api/v1/users/forgot-password").contentType("application/json").content(body("email",email))).andExpect(status().isAccepted());
        String token=latestToken(email),replacement="new safe passphrase 456!";
        mvc.perform(post("/api/v1/users/reset-password").contentType("application/json").content(body("token",token,"password","short"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/users/reset-password").contentType("application/json").content(body("token",token,"password",replacement))).andExpect(status().isOk());
        for(String auth:List.of(first,second)) mvc.perform(get("/api/v1/users/me").header("Authorization",auth)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/users/login").contentType("application/json").content(body("email",email,"password",password))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/users/login").contentType("application/json").content(body("email",email,"password",replacement))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/users/reset-password").contentType("application/json").content(body("token",token,"password",password))).andExpect(status().isBadRequest());
    }
    @Test void concurrentConsumptionHasOnlyOneWinner() throws Exception {
        String email=email();signup(email);String token=latestToken(email);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var gate=new CountDownLatch(1);
            Callable<Boolean> action=() -> { gate.await(); try {accounts.verify(new AccountRequests.VerifyRequest(token,"동시검증",password));return true;}catch(IllegalArgumentException e){return false;} };
            Future<Boolean> a=pool.submit(action), b=pool.submit(action);gate.countDown();
            assertThat(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        }
    }
    @Test void deletionRequiresPasswordAndConfirmationAndRemovesOnlyRelatedData() throws Exception {
        String email=email(),otherEmail=email();signup(email);activate(email);signup(otherEmail);activate(otherEmail);
        String first=login(email),second=login(email),otherAuth=login(otherEmail);
        var owner=users.findByEmail(email).orElseThrow();var other=users.findByEmail(otherEmail).orElseThrow();
        var ownQuestion=questions.save(Question.builder().title("내 질문").content("삭제 대상").user(owner).build());
        var otherQuestion=questions.save(Question.builder().title("다른 질문").content("유지 대상").user(other).build());
        answers.save(Answer.builder().question(ownQuestion).user(other).content("내 질문의 답변").build());
        answers.save(Answer.builder().question(otherQuestion).user(owner).content("내가 쓴 답변").build());
        var kept=answers.save(Answer.builder().question(otherQuestion).user(other).content("유지 답변").build());
        mvc.perform(post("/api/v1/users/delete-account").header("Authorization",first).contentType("application/json").content(body("password",password,"confirmation","no"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/users/delete-account").header("Authorization",first).contentType("application/json").content(body("password","wrong-password","confirmation","회원 탈퇴"))).andExpect(status().isBadRequest());
        assertThat(users.existsById(owner.getId())).isTrue();
        mvc.perform(post("/api/v1/users/delete-account").header("Authorization",first).contentType("application/json").content(body("password",password,"confirmation","회원 탈퇴"))).andExpect(status().isOk());
        assertThat(users.existsById(owner.getId())).isFalse();assertThat(questions.existsById(ownQuestion.getId())).isFalse();
        assertThat(questions.existsById(otherQuestion.getId())).isTrue();assertThat(answers.existsById(kept.getId())).isTrue();
        assertThat(answers.findByQuestionIdOrderByIdAsc(otherQuestion.getId())).hasSize(1);
        assertThat(tokens.findAll().stream().filter(t -> t.getUserId().equals(owner.getId()))).isEmpty();
        for(String auth:List.of(first,second)) mvc.perform(get("/api/v1/users/me").header("Authorization",auth)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/me").header("Authorization",otherAuth)).andExpect(status().isOk());
    }
}
