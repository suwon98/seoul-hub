package org.seoulhub.backend.domain.congestion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiSpecExtractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void extractOpenApiSpecJson() throws Exception {
        // 1. 가상 서블릿 엔진 내부의 4.0 OpenAPI 명세 추출
        String openApiJson = mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        // 2. 모노레포 루트 경로 최상위에 openapi.json 물리 자산 드롭
        if (openApiJson != null && !openApiJson.contains("error")) {
            Files.writeString(Paths.get("../../openapi.json"), openApiJson);
            System.out.println("=== [SUCCESS] openapi.json 자산 추출 완료 ===");
        }
    }
}