package org.seoulhub.backend;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class ApiFlowTests extends AccountTestSupport {
    @Test void signupVerifyLoginQuestionAnswerAndValidation() throws Exception {
        String email=email(); signup(email); activate(email); String auth=login(email);
        var question=mvc.perform(post("/api/v1/questions").header("Authorization",auth).contentType("application/json")
            .content(body("title","서울 방문 질문","content","언제 방문하면 좋을까요?"))).andExpect(status().isOk()).andReturn().getResponse();
        long id=json.readTree(question.getContentAsString()).path("questionId").asLong();
        mvc.perform(get("/api/v1/questions/"+id).header("Authorization",auth)).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("서울 방문 질문")).andExpect(jsonPath("$.writerEmail").doesNotExist());
        mvc.perform(post("/api/v1/questions/"+id+"/answers").header("Authorization",auth).contentType("application/json").content(body("content","아침을 추천해요."))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/questions/"+id+"/answers").header("Authorization",auth)).andExpect(jsonPath("$[0].content").value("아침을 추천해요.")).andExpect(jsonPath("$[0].writerEmail").doesNotExist());
        mvc.perform(post("/api/v1/questions").header("Authorization",auth).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
    }
    @Test void publicDataAndProtectedCommunity() throws Exception {
        mvc.perform(get("/api/v1/questions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/questions").header("Authorization","Bearer invalid")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/congestion/all")).andExpect(status().isOk()).andExpect(jsonPath("$[0].congestionLevel").value("UNKNOWN"));
        mvc.perform(get("/api/v1/congestion/강남역/history")).andExpect(status().isOk());
    }
}
