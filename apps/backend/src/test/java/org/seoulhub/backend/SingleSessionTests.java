package org.seoulhub.backend;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

class SingleSessionTests extends AccountTestSupport {
    @Test void newLoginRevokesOldSessionButFailedLoginAndOtherUsersDoNot() throws Exception {
        String email=email(),other=email();signup(email);activate(email);signup(other);activate(other);
        String first=login(email),otherToken=login(other);
        mvc.perform(post("/api/v1/users/login").contentType("application/json").content(body("email",email,"password","incorrect-password"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/me").header("Authorization",first)).andExpect(status().isOk());
        String latest=login(email.toUpperCase());
        mvc.perform(get("/api/v1/users/me").header("Authorization",first)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/users/logout").header("Authorization",first)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/me").header("Authorization",latest)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/users/me").header("Authorization",otherToken)).andExpect(status().isOk());
        assertThat(sessions.findAll().stream().filter(s -> s.getEmail().equals(email)).count()).isEqualTo(1);
    }
    @Test void simultaneousLoginsLeaveExactlyOneValidSession() throws Exception {
        String email=email();signup(email);activate(email);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var gate=new CountDownLatch(1);
            Callable<String> action=() -> {gate.await();return login(email);};
            var first=pool.submit(action);var second=pool.submit(action);gate.countDown();
            String a=first.get(15,TimeUnit.SECONDS),b=second.get(15,TimeUnit.SECONDS);
            int sa=mvc.perform(get("/api/v1/users/me").header("Authorization",a)).andReturn().getResponse().getStatus();
            int sb=mvc.perform(get("/api/v1/users/me").header("Authorization",b)).andReturn().getResponse().getStatus();
            assertThat(java.util.List.of(sa,sb)).containsExactlyInAnyOrder(200,401);
            assertThat(sessions.findAll().stream().filter(s -> s.getEmail().equals(email)).count()).isEqualTo(1);
        }
    }
}
