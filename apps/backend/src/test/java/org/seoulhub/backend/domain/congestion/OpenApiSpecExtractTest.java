package org.seoulhub.backend.domain.congestion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootTest(properties = "springdoc.api-docs.enabled=true")
@AutoConfigureMockMvc
class OpenApiSpecExtractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void extractOpenApiSpecJson() throws Exception {
        String openApiJson = mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        Path current = Paths.get(".").toAbsolutePath();
        Path targetPath = null;

        while (current != null) {
            if (Files.exists(current.resolve(".git")) || Files.exists(current.resolve("scripts"))) {
                targetPath = current.resolve("apps/backend/build/openapi.json");
                break;
            }
            current = current.getParent();
        }

        if (targetPath == null) {
            targetPath = Paths.get("build/openapi.json").toAbsolutePath();
        }

        Files.createDirectories(targetPath.getParent());
        Files.writeString(targetPath, openApiJson);
        System.out.println("=== [TARGET MATCH SUCCESS] openapi.json saved to: " + targetPath.toAbsolutePath() + " ===");
    }
}