package org.seoulhub.backend;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.seoulhub.backend.global.mail.AccountMailSender;
import org.seoulhub.backend.domain.user.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
abstract class AccountTestSupport {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired AccountTokenRepository tokens;
    @Autowired LoginSessionRepository sessions;
    @MockitoBean AccountMailSender mail;
    @Autowired org.seoulhub.backend.global.config.RequestRateLimiter limiter;
    final ObjectMapper json=new ObjectMapper();
    final Map<String,String> mailbox=new ConcurrentHashMap<>();
    final String password="long pass phrase 123!";
    @BeforeEach void captureMail() {
        mailbox.clear();
        ((Map<?,?>)org.springframework.test.util.ReflectionTestUtils.getField(limiter,"windows")).clear();
        doAnswer(invocation -> { mailbox.put(invocation.getArgument(0),invocation.getArgument(2)); return null; })
            .when(mail).send(anyString(),anyString(),anyString());
    }
    String email() {return "account-"+UUID.randomUUID()+"@example.com";}
    String body(Object... pairs) throws Exception {
        Map<String,Object> result=new LinkedHashMap<>();
        for(int i=0;i<pairs.length;i+=2) result.put((String)pairs[i],pairs[i+1]);
        return json.writeValueAsString(result);
    }
    String latestToken(String email) {
        var match=java.util.regex.Pattern.compile("#token=([A-Za-z0-9_-]{43})").matcher(mailbox.get(email));
        if(!match.find()) throw new AssertionError("Expected one-time email link");
        return match.group(1);
    }
    void signup(String email) throws Exception {
        mvc.perform(post("/api/v1/users/signup").contentType("application/json").content(body("email",email))).andExpect(status().isAccepted());
    }
    void activate(String email) throws Exception {
        mvc.perform(post("/api/v1/users/verify-email").contentType("application/json")
            .content(body("token",latestToken(email),"name","테스트회원","password",password))).andExpect(status().isOk());
    }
    String login(String email) throws Exception {
        var result=mvc.perform(post("/api/v1/users/login").contentType("application/json")
            .content(body("email",email,"password",password))).andExpect(status().isOk()).andReturn().getResponse();
        return "Bearer "+json.readTree(result.getContentAsString()).path("accessToken").asText();
    }
}
